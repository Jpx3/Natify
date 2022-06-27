@ECHO OFF
cd output/native
rmdir win /s /q
mkdir win

for %%f in (*.c) do (
  echo Compiling "%%f"...
  gcc -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32" -m64 -std=c99 -fPIC -shared -Oz -s %%f -o win/%%~nf.natify
  echo Done
)
cd ../..
