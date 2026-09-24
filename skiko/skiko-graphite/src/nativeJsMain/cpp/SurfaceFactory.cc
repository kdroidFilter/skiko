#include "common.h"
#include "GraphiteRecorder.hh"

#include "include/core/SkColorSpace.h"
#include "include/gpu/graphite/BackendTexture.h"
#include "include/gpu/graphite/Surface.h"

SKIKO_EXPORT KNativePointer org_jetbrains_skia_gpu_graphite_SurfaceFactory__1nWrapBackendTexture(
        KNativePointer recorderPtr,
        KNativePointer backendTexturePtr,
        KNativePointer colorSpacePtr,
        KInteropPointer surfacePropsValues) {
    auto recorder = reinterpret_cast<SkikoGraphiteRecorder*>(recorderPtr);
    if (!recorder) return nullptr;
    auto backendTexture = reinterpret_cast<skgpu::graphite::BackendTexture*>(backendTexturePtr);
    auto colorSpace = sk_ref_sp(reinterpret_cast<SkColorSpace*>(colorSpacePtr));
    auto surfaceProps = skija::SurfaceProps::toSkSurfaceProps(surfacePropsValues);
    return reinterpret_cast<KNativePointer>(recorder->track(SkSurfaces::WrapBackendTexture(
            recorder->get(),
            *backendTexture,
            std::move(colorSpace),
            surfaceProps.get())).release());
}
