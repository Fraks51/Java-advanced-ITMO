#!/bin/bash
dir=$PWD
resource_path=$dir/info/kgeorgiy/java/advanced/implementor
implementor_path=$dir/ru/ifmo/rain/zhuvertcev/implementor

javadoc -d _javadoc -link https://docs.oracle.com/en/java/javase/11/docs/api/ --module-path "$dir/lib":"$dir/artifacts" -private -author "$implementor_path/Implementor.java" "$implementor_path/JarImplementor.java" "$resource_path/Impler.java" "$resource_path/JarImpler.java" "$resource_path/ImplerException.java"