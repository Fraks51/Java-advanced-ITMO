#!/bin/bash
cd ../..
dir=$PWD

resource_path=$dir/info/kgeorgiy/java/advanced/implementor
implementor_path=$dir/ru/ifmo/rain/zhuvertcev/implementor


#SET mod_name=ru.ifmo.rain.oreshnikov.implementor
#SET mod_path=ru\ifmo\rain\oreshnikov\implementor
#
#SET wd=
#SET out=%wd%\out\production\java-2019
#SET req=%wd%\lib;%wd%\artifacts
#
#SET src=%wd%\my.modules\%mod_name%
#SET run=%wd%\run\implementor

javac --module-path "$dir/lib":"$dir/artifacts" %src%/module-info.java %src%\%mod_path%\*.java -d %out%
cd %out%
jar -c --file=%run%\Implementor.jar --main-class=%mod_name%.Implementor --module-path=%req% module-info.class %mod_path%\*.class
cd %run%