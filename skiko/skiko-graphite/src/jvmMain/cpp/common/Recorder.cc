#include <jni.h>

#include "GraphiteRecorder.hh"

static void deleteRecorder(SkikoGraphiteRecorder* recorder) {
    delete recorder;
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_jetbrains_skia_gpu_graphite_RecorderKt__1nGetRecorderFinalizer(JNIEnv*, jclass) {
    return static_cast<jlong>(reinterpret_cast<uintptr_t>(&deleteRecorder));
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_jetbrains_skia_gpu_graphite_RecorderKt__1nSnap(
        JNIEnv*, jclass, jlong recorderPtr) {
    auto recorder = reinterpret_cast<SkikoGraphiteRecorder*>(
            static_cast<uintptr_t>(recorderPtr));
    return reinterpret_cast<jlong>(recorder->snap().release());
}
