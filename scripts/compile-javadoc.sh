#!/bin/bash
cd ..
script_dir=$PWD
cd ../..
dir=$PWD

mod_path=ru/ifmo/rain/zhuvertcev/implementor
java_advanced=$dir/java-advanced-2020
implementor_path=$script_dir/$mod_path
resource_path=$java_advanced/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor

javadoc -d _javadoc -link https://docs.oracle.com/en/java/javase/11/docs/api/ --module-path "$java_advanced/lib":"$java_advanced/artifacts" -private -author "$implementor_path/Implementor.java" "$implementor_path/JarImplementor.java" "$resource_path/Impler.java" "$resource_path/JarImpler.java" "$resource_path/ImplerException.java"
