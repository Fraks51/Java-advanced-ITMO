#!/bin/bash
cd ..
script_dir=$PWD
cd ../..
dir=$PWD

mod_path=ru/ifmo/rain/zhuvertcev/implementor
java_advanced=$dir/java-advanced-2020
implementor_path=$script_dir/$mod_path

#компиляция в _build
javac --module-path "$java_advanced/lib":"$java_advanced/artifacts"  "$script_dir/module-info.java" "$script_dir/$mod_path/Implementor.java" "$script_dir/$mod_path/JarImplementor.java" -d "$script_dir/_build"

output_dir=$script_dir/_build
cd $output_dir

#создание jar
jar -c --file="$script_dir/Implementor.jar" --main-class=ru.ifmo.rain.zhuvertcev.implementor.Implementor --module-path="$java_advanced/lib":"$java_advanced/artifacts" module-info.class "$mod_path/Implementor.class" "$mod_path/JarImplementor.class"
