#include "common.h"

#include "GraphiteRecorder.hh"

static void deleteRecorder(SkikoGraphiteRecorder* recorder) {
    delete recorder;
}

SKIKO_EXPORT KNativePointer org_jetbrains_skia_gpu_graphite_Recorder__1nGetFinalizer() {
    return reinterpret_cast<KNativePointer>(&deleteRecorder);
}

SKIKO_EXPORT KNativePointer org_jetbrains_skia_gpu_graphite_Recorder__1nSnap(
        KNativePointer recorderPtr) {
    auto recorder = reinterpret_cast<SkikoGraphiteRecorder*>(recorderPtr);
    return reinterpret_cast<KNativePointer>(recorder->snap().release());
}
