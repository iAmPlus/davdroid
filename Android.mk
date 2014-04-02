LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := android-common \
		android-support-v4 \
		libbackport-util-concurrent \
		libcommons-lang \
		libical4j \
		libcommons-codec \
		libcommons-logging \
		liblombok-api \
		libcommons-io \
		libez-vcard \
		libhttpclientandroid \
		libsimple-xml

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res

LOCAL_PACKAGE_NAME := IamPlusSyncAdapter

LOCAL_CLASSPATH := $(LOCAL_PATH)/libs/lombok.jar

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
		libbackport-util-concurrent:libs/backport-util-concurrent-3.1.jar \
		libcommons-lang:libs/commons-lang-2.6.jar \
		libical4j:libs/ical4j-1.0.5.2.jar \
		libcommons-codec:libs/commons-codec-1.8.jar \
		libcommons-logging:libs/commons-logging-1.1.3.jar \
		liblombok-api:libs/lombok-api.jar \
		libcommons-io:libs/commons-io-2.4.jar \
		libez-vcard:libs/ez-vcard-0.9.2.jar \
		libhttpclientandroid:libs/httpclientandroidlib-1.2.1.jar \
		libsimple-xml:libs/simple-xml-2.7.jar

#LOCAL_PREBUILT_JAVA_LIBRARIES := liblombok:libs/lombok.jar

include $(BUILD_MULTI_PREBUILT)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
