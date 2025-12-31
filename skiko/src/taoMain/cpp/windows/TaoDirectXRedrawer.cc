#ifdef SK_DIRECT3D
#include <Windows.h>
#include "jni_helpers.h"
#include "exceptions_handler.h"
#include "window_util.h"

#include "SkColorSpace.h"
#include "ganesh/GrBackendSurface.h"
#include "ganesh/GrDirectContext.h"
#include "SkSurface.h"
#include "include/gpu/ganesh/SkSurfaceGanesh.h"
#include "interop.hh"
#include "DCompLibrary.h"

#include "ganesh/d3d/GrD3DTypes.h"
#include <d3d12.h>
#include <dxgi1_4.h>
#include <dxgi1_6.h>

static HRESULT callD3D12CreateDevice(
    IUnknown *pAdapter,
    D3D_FEATURE_LEVEL minimumFeatureLevel,
    REFIID riid,
    void **ppDevice) {
    typedef HRESULT (*D3D12CreateDevice_t)(
        IUnknown * pAdapter,
        D3D_FEATURE_LEVEL MinimumFeatureLevel,
        REFIID riid,
        void **ppDevice);
    static D3D12CreateDevice_t impl = nullptr;
    if (!impl) {
        auto d3d12dll = LoadLibrary(TEXT("D3D12.dll"));
        if (!d3d12dll)
            return E_NOTIMPL;
        impl = (D3D12CreateDevice_t)GetProcAddress(d3d12dll, "D3D12CreateDevice");
        if (!impl)
            return E_NOTIMPL;
    }
    return impl(pAdapter, minimumFeatureLevel, riid, ppDevice);
}

static HRESULT callCreateDXGIFactory1(REFIID riid, void **ppFactory) {
    typedef HRESULT (*CreateDXGIFactory1_t)(REFIID riid, void **ppFactory);
    static CreateDXGIFactory1_t impl = nullptr;
    if (!impl) {
        auto dxgidll = LoadLibrary(TEXT("Dxgi.dll"));
        if (!dxgidll)
            return E_NOTIMPL;
        impl = (CreateDXGIFactory1_t)GetProcAddress(dxgidll, "CreateDXGIFactory1");
        if (!impl)
            return E_NOTIMPL;
    }
    return impl(riid, ppFactory);
}

static HRESULT callCreateDXGIFactory2(UINT flags, REFIID riid, void **ppFactory) {
    typedef HRESULT (*CreateDXGIFactory2_t)(UINT Flags, REFIID riid, void **ppFactory);
    static CreateDXGIFactory2_t impl = nullptr;
    if (!impl) {
        auto dxgidll = LoadLibrary(TEXT("Dxgi.dll"));
        if (!dxgidll)
            return E_NOTIMPL;
        impl = (CreateDXGIFactory2_t)GetProcAddress(dxgidll, "CreateDXGIFactory2");
        if (!impl)
            return E_NOTIMPL;
    }
    return impl(flags, riid, ppFactory);
}

static const int BuffersCount = 2;

class DirectXDevice {
public:
    HWND hWnd;
    GrD3DBackendContext backendContext;
    gr_cp<ID3D12Device> device;
    gr_cp<IDXGISwapChain3> swapChain;
    gr_cp<ID3D12CommandQueue> queue;
    gr_cp<ID3D12Resource> buffers[BuffersCount];
    gr_cp<ID3D12Fence> fence;
    uint64_t fenceValues[BuffersCount];
    HANDLE fenceEvent = NULL;
    unsigned int bufferIndex = 0;

    ~DirectXDevice() {
        if (fenceEvent != NULL) {
            CloseHandle(fenceEvent);
        }
        for (int i = 0; i < BuffersCount; i++) {
            buffers[i].reset(nullptr);
        }
        fence.reset(nullptr);
        swapChain.reset(nullptr);
        queue.reset(nullptr);
        device.reset(nullptr);
    }

    void initSwapChain(UINT width, UINT height, jboolean transparency) {
        gr_cp<IDXGIFactory4> swapChainFactory4;
        gr_cp<IDXGISwapChain1> swapChain1;
        callCreateDXGIFactory2(0, IID_PPV_ARGS(&swapChainFactory4));
        HRESULT result = S_OK;
        if (transparency) {
            result = CreateSwapChainForComposition(swapChainFactory4.get(), width, height, &swapChain1);
        }
        if (!transparency || FAILED(result)) {
            swapChain1.reset(nullptr);
            CreateSwapChainForHwnd(swapChainFactory4.get(), width, height, &swapChain1);
        }
        swapChainFactory4->MakeWindowAssociation(hWnd, DXGI_MWA_NO_ALT_ENTER);
        swapChain1->QueryInterface(IID_PPV_ARGS(&swapChain));
        swapChainFactory4.reset(nullptr);
    }

private:
    HRESULT CreateSwapChainForComposition(IDXGIFactory4 *swapChainFactory4, UINT width, UINT height, IDXGISwapChain1 **swapChain1) {
        DXGI_SWAP_CHAIN_DESC1 swapChainDesc = {};
        swapChainDesc.Width = width;
        swapChainDesc.Height = height;
        swapChainDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
        swapChainDesc.SampleDesc.Count = 1;
        swapChainDesc.SampleDesc.Quality = 0;
        swapChainDesc.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
        swapChainDesc.BufferCount = BuffersCount;
        swapChainDesc.Scaling = DXGI_SCALING_STRETCH;
        swapChainDesc.SwapEffect = DXGI_SWAP_EFFECT_FLIP_DISCARD;
        swapChainDesc.AlphaMode = DXGI_ALPHA_MODE_PREMULTIPLIED;
        HRESULT result = swapChainFactory4->CreateSwapChainForComposition(queue.get(), &swapChainDesc, nullptr, swapChain1);
        if (FAILED(result)) { return result; }

        gr_cp<IDCompositionDevice> dcDevice;
        gr_cp<IDCompositionTarget> dcTarget;
        gr_cp<IDCompositionVisual> dcVisual;
        result = DCompLibrary::DCompositionCreateDevice(0, IID_PPV_ARGS(&dcDevice));
        if (FAILED(result)) { return result; }
        result = dcDevice->CreateTargetForHwnd(hWnd, true, &dcTarget);
        if (FAILED(result)) { return result; }
        result = dcDevice->CreateVisual(&dcVisual);
        if (FAILED(result)) { return result; }
        result = dcVisual->SetContent(*swapChain1);
        if (FAILED(result)) { return result; }
        result = dcTarget->SetRoot(dcVisual.get());
        if (FAILED(result)) { return result; }
        result = dcDevice->Commit();
        return result;
    }

    HRESULT CreateSwapChainForHwnd(IDXGIFactory4 *swapChainFactory4, UINT width, UINT height, IDXGISwapChain1 **swapChain1) {
        DXGI_SWAP_CHAIN_DESC1 swapChainDesc = {};
        swapChainDesc.Width = width;
        swapChainDesc.Height = height;
        swapChainDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
        swapChainDesc.SampleDesc.Count = 1;
        swapChainDesc.SampleDesc.Quality = 0;
        swapChainDesc.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
        swapChainDesc.BufferCount = BuffersCount;
        swapChainDesc.Scaling = DXGI_SCALING_STRETCH;
        swapChainDesc.SwapEffect = DXGI_SWAP_EFFECT_FLIP_DISCARD;
        return swapChainFactory4->CreateSwapChainForHwnd(queue.get(), hWnd, &swapChainDesc, nullptr, nullptr, swapChain1);
    }
};

extern "C" {
    JNIEXPORT jlong JNICALL Java_org_jetbrains_skiko_tao_TaoDirectXBackend_createDirectXDevice(
        JNIEnv *env, jobject redrawer, jlong hwndLong, jboolean transparency, jint /*frameBuffering*/) {
        gr_cp<IDXGIFactory4> deviceFactory;
        if (!SUCCEEDED(callCreateDXGIFactory1(IID_PPV_ARGS(&deviceFactory)))) {
            return 0;
        }

        gr_cp<IDXGIAdapter1> adapter;
        if (FAILED(deviceFactory->EnumAdapters1(0, &adapter))) {
            return 0;
        }

        D3D_FEATURE_LEVEL maxSupportedFeatureLevel = D3D_FEATURE_LEVEL_12_0;
        D3D_FEATURE_LEVEL featureLevels[] = {
            D3D_FEATURE_LEVEL_12_1,
            D3D_FEATURE_LEVEL_12_0
        };

        for (int i = 0; i < _countof(featureLevels); i++) {
            if (SUCCEEDED(callD3D12CreateDevice(adapter.get(), featureLevels[i], _uuidof(ID3D12Device), nullptr))) {
                maxSupportedFeatureLevel = featureLevels[i];
                break;
            }
        }

        gr_cp<ID3D12Device> device;
        if (!SUCCEEDED(callD3D12CreateDevice(adapter.get(), maxSupportedFeatureLevel, IID_PPV_ARGS(&device)))) {
            return 0;
        }

        gr_cp<ID3D12CommandQueue> queue;
        D3D12_COMMAND_QUEUE_DESC queueDesc = {};
        queueDesc.Flags = D3D12_COMMAND_QUEUE_FLAG_NONE;
        queueDesc.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
        if (!SUCCEEDED(device->CreateCommandQueue(&queueDesc, IID_PPV_ARGS(&queue)))) {
            return 0;
        }

        HWND hWnd = fromJavaPointer<HWND>(hwndLong);
        DirectXDevice *d3dDevice = new DirectXDevice();
        d3dDevice->backendContext.fAdapter = adapter;
        d3dDevice->backendContext.fDevice = device;
        d3dDevice->backendContext.fQueue = queue;
        d3dDevice->backendContext.fProtectedContext = GrProtected::kNo;

        d3dDevice->device = device;
        d3dDevice->queue = queue;
        d3dDevice->hWnd = hWnd;

        if (transparency) {
            const LONG style = GetWindowLong(hWnd, GWL_EXSTYLE);
            SetWindowLong(hWnd, GWL_EXSTYLE, style | WS_EX_TRANSPARENT);
        }

        d3dDevice->initSwapChain(1, 1, transparency);
        d3dDevice->bufferIndex = d3dDevice->swapChain->GetCurrentBackBufferIndex();
        d3dDevice->swapChain->GetBuffer(d3dDevice->bufferIndex, IID_PPV_ARGS(&d3dDevice->buffers[d3dDevice->bufferIndex]));

        return toJavaPointer(d3dDevice);
    }

    JNIEXPORT jlong JNICALL Java_org_jetbrains_skiko_tao_TaoDirectXBackend_makeDirectXContext(
        JNIEnv *, jobject, jlong devicePtr) {
        DirectXDevice *d3dDevice = fromJavaPointer<DirectXDevice *>(devicePtr);
        GrD3DBackendContext backendContext = d3dDevice->backendContext;
        return toJavaPointer(GrDirectContext::MakeDirect3D(backendContext).release());
    }

    JNIEXPORT jlong JNICALL Java_org_jetbrains_skiko_tao_TaoDirectXBackend_makeDirectXRenderTarget(
        JNIEnv *, jobject, jlong devicePtr, jint width, jint height, jint /*bufferIndex*/) {
        DirectXDevice *d3dDevice = fromJavaPointer<DirectXDevice *>(devicePtr);
        UINT currentIndex = d3dDevice->swapChain->GetCurrentBackBufferIndex();
        d3dDevice->bufferIndex = currentIndex;
        d3dDevice->swapChain->GetBuffer(currentIndex, IID_PPV_ARGS(&d3dDevice->buffers[currentIndex]));

        GrD3DTextureResourceInfo info(nullptr,
                                      nullptr,
                                      D3D12_RESOURCE_STATE_PRESENT,
                                      DXGI_FORMAT_R8G8B8A8_UNORM,
                                      1,
                                      1,
                                      0);
        info.fResource = d3dDevice->buffers[currentIndex];
        GrBackendRenderTarget *rt = new GrBackendRenderTarget(
            width,
            height,
            info
        );
        return toJavaPointer(rt);
    }

    JNIEXPORT void JNICALL Java_org_jetbrains_skiko_tao_TaoDirectXBackend_finishFrame(
        JNIEnv *env, jobject, jlong devicePtr) {
        __try {
            DirectXDevice *d3dDevice = fromJavaPointer<DirectXDevice *>(devicePtr);
            d3dDevice->swapChain->Present(1, 0);
        } __except(EXCEPTION_EXECUTE_HANDLER) {
            auto code = GetExceptionCode();
            throwJavaRenderExceptionByExceptionCode(env, __FUNCTION__, code);
        }
    }

    JNIEXPORT void JNICALL Java_org_jetbrains_skiko_tao_TaoDirectXBackend_resizeSwapchain(
        JNIEnv *env, jobject, jlong devicePtr, jint width, jint height) {
        __try {
            DirectXDevice *d3dDevice = fromJavaPointer<DirectXDevice *>(devicePtr);
            for (int i = 0; i < BuffersCount; i++) {
                d3dDevice->buffers[i].reset(nullptr);
            }
            d3dDevice->swapChain->ResizeBuffers(BuffersCount, width, height, DXGI_FORMAT_R8G8B8A8_UNORM, 0);
        } __except(EXCEPTION_EXECUTE_HANDLER) {
            auto code = GetExceptionCode();
            throwJavaRenderExceptionByExceptionCode(env, __FUNCTION__, code);
        }
    }

    JNIEXPORT void JNICALL Java_org_jetbrains_skiko_tao_TaoDirectXBackend_disposeDevice(
        JNIEnv *, jobject, jlong devicePtr) {
        DirectXDevice *d3dDevice = fromJavaPointer<DirectXDevice *>(devicePtr);
        delete d3dDevice;
    }
}
#endif
