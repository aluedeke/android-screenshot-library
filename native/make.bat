@ set GCC=arm-none-linux-gnueabi-gcc.exe
@ set C_FLAGS=-DDEBUG
@ set ADB=C:\Android\tools\adb.exe

REM Compiling...
@ %GCC% %C_FLAGS% -c main.c -o main.o
	@if not ERRORLEVEL 0 goto Error
@ %GCC% %C_FLAGS% -c fbshot.c -o fbshot.o
	@if not ERRORLEVEL 0 goto Error

REM Linking...
@ %GCC% --static fbshot.o main.o -o asl-native
@if not ERRORLEVEL 0 goto Error

REM Pushing to device...
@ %ADB% push ./asl-native /data/local/asl-native

REM Running...
@ %ADB% shell /system/bin/chmod 0777 /data/local/asl-native
@ %ADB% shell "/data/local/asl-native &"

@goto Exit

:Error
REM Error while compiling/linking.

:Exit