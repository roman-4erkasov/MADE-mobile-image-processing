LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#opencv
OPENCVROOT:= /Users/ruaihm4/code/made/mobile/MADE-mobile-image-processing/lesson2/src/ImageProcessing/
OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
OPENCV_LIB_TYPE:=SHARED
include ${OPENCVROOT}/sdk/native/jni/OpenCV.mk

LOCAL_SRC_FILES := com_asav_processimage_MainActivity.cpp
LOCAL_CFLAGS += -mfloat-abi=softfp -mfpu=neon -std=c++11 #-march=armv64
LOCAL_ARM_NEON  := true
LOCAL_LDLIBS += -llog
LOCAL_MODULE := OpenCvProcessImageLib

include $(BUILD_SHARED_LIBRARY)