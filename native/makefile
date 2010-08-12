# Android Screenshot Library
# -- Native Android application --
# Makefile

x86_GCC=gcc
ARM_GCC=/opt/local/arm-2010q1/bin/arm-none-linux-gnueabi-gcc

C_FLAGS=-DDEBUG
L_FLAGS=--static

ADB=/var/lib/google/android-sdk-linux_86/tools/adb
DEVICE_FILE=/data/local/asl-native


native:	fbshot main
	${ARM_GCC} ${L_FLAGS} fbshot.o main.o -o native

fbshot:	fbshot.c common.h
	${ARM_GCC} -c ${C_FLAGS} fbshot.c -o fbshot.o

main:	main.c common.h
	${ARM_GCC} -c ${C_FLAGS} main.c -o main.o


clean:
	rm native*
	rm *.o


upload: native
	${ADB} push ./native ${DEVICE_FILE}


x86:	main.c fbshot.c common.h
	${x86_GCC} -c ${C_FLAGS} main.c -o main-x86.o
	${x86_GCC} -c ${C_FLAGS} fbshot.c -o fbshot-x86.o
	${x86_GCC} ${L_FLAGS} fbshot-x86.o main-x86.o -o native-x86
