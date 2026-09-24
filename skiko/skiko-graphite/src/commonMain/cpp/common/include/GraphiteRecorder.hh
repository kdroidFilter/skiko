#pragma once

#include "include/core/SkRefCnt.h"
#include "include/core/SkSurface.h"
#include "include/gpu/graphite/Recorder.h"
#include "include/gpu/graphite/Recording.h"

#include <memory>
#include <vector>

// Graphite Recorder that keeps an extra reference to every Surface wrapped for it.
//
// Skia requires a Surface to be destroyed on its Recorder's thread, because ~Surface flushes the
// Surface's pending work into the Recorder. Skiko finalizers run on a dedicated cleaner thread, so
// an unclosed Surface would otherwise be destroyed concurrently with the Recorder being used.
// With the extra reference, the finalizer never releases the last one: the Surface is destroyed on
// the Recorder's thread by the next snap() or wrap, once nothing else references it.
class SkikoGraphiteRecorder {
public:
    explicit SkikoGraphiteRecorder(std::unique_ptr<skgpu::graphite::Recorder> recorder);

    ~SkikoGraphiteRecorder();

    skgpu::graphite::Recorder* get() const { return fRecorder.get(); }

    // Takes an extra reference to surface and returns it.
    sk_sp<SkSurface> track(sk_sp<SkSurface> surface);

    std::unique_ptr<skgpu::graphite::Recording> snap();

private:
    void releaseUnusedSurfaces();

    std::unique_ptr<skgpu::graphite::Recorder> fRecorder;
    std::vector<sk_sp<SkSurface>> fSurfaces;
};
