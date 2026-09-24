#include <jni.h>

#include "interop.hh"
#include "GraphiteRecorder.hh"
#include "include/core/SkColorSpace.h"
#include "include/gpu/graphite/BackendTexture.h"
#include "include/gpu/graphite/Surface.h"

extern "C" JNIEXPORT jlong JNICALL
Java_org_jetbrains_skia_gpu_graphite_SurfaceFactoryKt__1nWrapBackendTexture(
        JNIEnv* env,
        jclass,
        jlong recorderPtr,
        jlong backendTexturePtr,
        jlong colorSpacePtr,
        jintArray surfacePropsValues) {
    auto recorder = reinterpret_cast<SkikoGraphiteRecorder*>(
            static_cast<uintptr_t>(recorderPtr));
    if (!recorder) return 0;
    auto backendTexture = reinterpret_cast<skgpu::graphite::BackendTexture*>(
            static_cast<uintptr_t>(backendTexturePtr));
    auto colorSpace = sk_ref_sp(reinterpret_cast<SkColorSpace*>(
            static_cast<uintptr_t>(colorSpacePtr)));
    auto surfaceProps = skija::SurfaceProps::toSkSurfaceProps(env, surfacePropsValues);
    return reinterpret_cast<jlong>(recorder->track(SkSurfaces::WrapBackendTexture(
            recorder->get(),
            *backendTexture,
            std::move(colorSpace),
            surfaceProps.get())).release());
}
