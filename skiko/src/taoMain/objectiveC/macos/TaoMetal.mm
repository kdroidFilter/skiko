#ifdef SK_METAL

#import <jni.h>
#import <Cocoa/Cocoa.h>
#import <QuartzCore/CAMetalLayer.h>
#import <QuartzCore/QuartzCore.h>
#import <Metal/Metal.h>

#import "ganesh/GrBackendSurface.h"
#import "ganesh/GrDirectContext.h"
#import "ganesh/mtl/GrMtlBackendContext.h"
#import "ganesh/mtl/GrMtlDirectContext.h"
#import "ganesh/mtl/GrMtlTypes.h"
#import "ganesh/mtl/GrMtlBackendSurface.h"

@interface TaoMetalDevice : NSObject
@property (strong) CAMetalLayer *layer;
@property (strong) id<MTLDevice> adapter;
@property (strong) id<MTLCommandQueue> queue;
@property (strong) id<CAMetalDrawable> drawableHandle;
@property (strong) dispatch_semaphore_t inflightSemaphore;
@end

@implementation TaoMetalDevice
@end

extern "C" {

JNIEXPORT jlong JNICALL Java_org_jetbrains_skiko_tao_TaoMetalBackend_createMetalDevice(
    JNIEnv *env, jobject backend, jlong viewPtr, jlong windowPtr, jboolean transparency, jint frameBuffering
) {
    @autoreleasepool {
        NSView *view = (__bridge NSView *)(void *) viewPtr;
        NSWindow *window = (__bridge NSWindow *)(void *) windowPtr;

        TaoMetalDevice *device = [TaoMetalDevice new];
        device.adapter = MTLCreateSystemDefaultDevice();
        device.queue = [device.adapter newCommandQueue];
        device.layer = [CAMetalLayer layer];
        if (frameBuffering == 2 || frameBuffering == 3) {
            device.layer.maximumDrawableCount = frameBuffering;
        }
        device.layer.device = device.adapter;
        device.layer.pixelFormat = MTLPixelFormatBGRA8Unorm;
        device.layer.opaque = !transparency;
        device.layer.framebufferOnly = NO;
        device.layer.contentsGravity = kCAGravityTopLeft;
        CGFloat scale = window ? window.backingScaleFactor : 1.0;
        device.layer.contentsScale = scale;

        device.inflightSemaphore = dispatch_semaphore_create(device.layer.maximumDrawableCount);

        view.wantsLayer = YES;
        view.layer = device.layer;
        // Don't set frame/drawableSize here - let resizeLayer handle it
        // This ensures consistency between init and resize

        return (jlong) CFBridgingRetain(device);
    }
}

JNIEXPORT jlong JNICALL Java_org_jetbrains_skiko_tao_TaoMetalBackend_makeMetalContext(
    JNIEnv *env, jobject backend, jlong devicePtr
) {
    @autoreleasepool {
        TaoMetalDevice *device = (__bridge TaoMetalDevice *)(void *) devicePtr;
        GrMtlBackendContext backendContext = {};
        backendContext.fDevice.retain((__bridge GrMTLHandle) device.adapter);
        backendContext.fQueue.retain((__bridge GrMTLHandle) device.queue);
        return (jlong) GrDirectContexts::MakeMetal(backendContext).release();
    }
}

JNIEXPORT jlong JNICALL Java_org_jetbrains_skiko_tao_TaoMetalBackend_makeMetalRenderTarget(
    JNIEnv *env, jobject backend, jlong devicePtr, jint width, jint height
) {
    @autoreleasepool {
        TaoMetalDevice *device = (__bridge TaoMetalDevice *)(void *) devicePtr;

        // Ensure drawableSize matches requested size before getting drawable
        CGSize currentSize = device.layer.drawableSize;
        if ((int)currentSize.width != width || (int)currentSize.height != height) {
            device.layer.drawableSize = CGSizeMake(width, height);
        }

        dispatch_semaphore_wait(device.inflightSemaphore, DISPATCH_TIME_FOREVER);

        id<CAMetalDrawable> currentDrawable = [device.layer nextDrawable];
        if (!currentDrawable) {
            dispatch_semaphore_signal(device.inflightSemaphore);
            return 0;
        }
        device.drawableHandle = currentDrawable;

        GrMtlTextureInfo info;
        info.fTexture.retain((__bridge GrMTLHandle) currentDrawable.texture);
        // Use passed dimensions - they should match drawableSize now
        GrBackendRenderTarget obj = GrBackendRenderTargets::MakeMtl(width, height, info);
        GrBackendRenderTarget *renderTarget = new GrBackendRenderTarget(obj);
        return (jlong) renderTarget;
    }
}

JNIEXPORT void JNICALL Java_org_jetbrains_skiko_tao_TaoMetalBackend_finishFrame(
    JNIEnv *env, jobject backend, jlong devicePtr
) {
    @autoreleasepool {
        TaoMetalDevice *device = (__bridge TaoMetalDevice *)(void *) devicePtr;
        id<CAMetalDrawable> currentDrawable = device.drawableHandle;
        if (currentDrawable) {
            id<MTLCommandBuffer> commandBuffer = [device.queue commandBuffer];
            commandBuffer.label = @"Present";
            [commandBuffer addCompletedHandler:^(id<MTLCommandBuffer> buffer) {
                dispatch_semaphore_signal(device.inflightSemaphore);
            }];
            [commandBuffer presentDrawable:currentDrawable];
            [commandBuffer commit];
            device.drawableHandle = nil;
        }
    }
}

JNIEXPORT void JNICALL Java_org_jetbrains_skiko_tao_TaoMetalBackend_resizeLayer(
    JNIEnv *env, jobject backend, jlong devicePtr, jint width, jint height, jfloat contentScale
) {
    @autoreleasepool {
        TaoMetalDevice *device = (__bridge TaoMetalDevice *)(void *) devicePtr;
        if (!device || !device.layer) return;

        // width/height are in physical pixels
        // frame should be in logical points (physical / scale)
        CGFloat logicalWidth = (CGFloat)width / contentScale;
        CGFloat logicalHeight = (CGFloat)height / contentScale;

        [CATransaction begin];
        [CATransaction setValue:(id)kCFBooleanTrue forKey:kCATransactionDisableActions];
        device.layer.frame = CGRectMake(0, 0, logicalWidth, logicalHeight);
        device.layer.contentsScale = contentScale;
        device.layer.drawableSize = CGSizeMake(width, height);
        [CATransaction commit];
        [CATransaction flush];
    }
}

JNIEXPORT void JNICALL Java_org_jetbrains_skiko_tao_TaoMetalBackend_disposeDevice(
    JNIEnv *env, jobject backend, jlong devicePtr
) {
    @autoreleasepool {
        TaoMetalDevice *device = CFBridgingRelease((void *) devicePtr);
        [device.layer removeFromSuperlayer];
    }
}

// ============================================================================
// System Theme Detection
// ============================================================================

JNIEXPORT jstring JNICALL Java_org_jetbrains_skiko_SystemTheme_1taoKt_getCurrentSystemThemeNative(
    JNIEnv *env, jclass clazz
) {
    @autoreleasepool {
        NSAppearance *appearance = [NSApp effectiveAppearance];
        NSAppearanceName bestMatch = [appearance bestMatchFromAppearancesWithNames:@[
            NSAppearanceNameAqua,
            NSAppearanceNameDarkAqua
        ]];

        if ([bestMatch isEqualToString:NSAppearanceNameDarkAqua]) {
            return env->NewStringUTF("dark");
        } else {
            return env->NewStringUTF("light");
        }
    }
}

// ============================================================================
// Clipboard via NSPasteboard
// ============================================================================

JNIEXPORT void JNICALL Java_org_jetbrains_skiko_ClipboardKt_setClipboardTextNative(
    JNIEnv *env, jclass clazz, jstring text
) {
    @autoreleasepool {
        if (text == NULL) return;

        const char *chars = env->GetStringUTFChars(text, NULL);
        if (chars == NULL) return;

        NSString *nsText = [NSString stringWithUTF8String:chars];
        env->ReleaseStringUTFChars(text, chars);

        NSPasteboard *pasteboard = [NSPasteboard generalPasteboard];
        [pasteboard clearContents];
        [pasteboard setString:nsText forType:NSPasteboardTypeString];
    }
}

JNIEXPORT jstring JNICALL Java_org_jetbrains_skiko_ClipboardKt_getClipboardTextNative(
    JNIEnv *env, jclass clazz
) {
    @autoreleasepool {
        NSPasteboard *pasteboard = [NSPasteboard generalPasteboard];
        NSString *text = [pasteboard stringForType:NSPasteboardTypeString];

        if (text == nil) {
            return NULL;
        }

        return env->NewStringUTF([text UTF8String]);
    }
}

JNIEXPORT jboolean JNICALL Java_org_jetbrains_skiko_ClipboardKt_hasClipboardTextNative(
    JNIEnv *env, jclass clazz
) {
    @autoreleasepool {
        NSPasteboard *pasteboard = [NSPasteboard generalPasteboard];
        NSArray *types = [pasteboard types];
        return [types containsObject:NSPasteboardTypeString] ? JNI_TRUE : JNI_FALSE;
    }
}

} // extern C

#endif
