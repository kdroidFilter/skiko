#include "GraphiteRecorder.hh"

#include <algorithm>

SkikoGraphiteRecorder::SkikoGraphiteRecorder(std::unique_ptr<skgpu::graphite::Recorder> recorder)
        : fRecorder(std::move(recorder)) {}

SkikoGraphiteRecorder::~SkikoGraphiteRecorder() {
    // Destroying the Recorder first abandons the devices of the remaining Surfaces, which makes
    // releasing them safe on any thread.
    fRecorder.reset();
    fSurfaces.clear();
}

sk_sp<SkSurface> SkikoGraphiteRecorder::track(sk_sp<SkSurface> surface) {
    this->releaseUnusedSurfaces();
    if (surface) {
        fSurfaces.push_back(surface);
    }
    return surface;
}

std::unique_ptr<skgpu::graphite::Recording> SkikoGraphiteRecorder::snap() {
    // Released Surfaces flush their pending work into the Recorder, so do it before snapping.
    this->releaseUnusedSurfaces();
    return fRecorder->snap();
}

void SkikoGraphiteRecorder::releaseUnusedSurfaces() {
    fSurfaces.erase(
            std::remove_if(fSurfaces.begin(), fSurfaces.end(),
                           [](const sk_sp<SkSurface>& surface) { return surface->unique(); }),
            fSurfaces.end());
}
