all:
	ant debug
	adb install -r bin/clapir-debug.apk
debug:
	ant debug
	adb install -r bin/clapir-debug.apk
	adb logcat -c
	adb logcat -s MainActivity ClapImpulseResponse MySurfaceView NowLayout TestCurve 
clean:
	ant clean
