cmake_minimum_required(VERSION 3.24)

project(tree-sitter-zenscript LANGUAGES C)

message(STATUS $ENV{JAVA_HOME})
cmake_path(SET JAVA_HOME $ENV{JAVA_HOME})
find_package(JNI REQUIRED)

if(WIN32) 
    set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS true)
endif()

add_library(tree-sitter STATIC)

target_sources(tree-sitter PUBLIC tree-sitter/lib/src/lib.c)
target_include_directories(tree-sitter PUBLIC tree-sitter/lib/include)

add_library(zenscript-grammar STATIC)

target_sources(zenscript-grammar PUBLIC src/parser.c)
target_include_directories(zenscript-grammar PRIVATE src)
target_link_libraries(zenscript-grammar PUBLIC JNI::JNI )


add_library(tree-sitter-zenscript SHARED)
target_sources(zenscript-grammar PUBLIC interop/helper.c)
target_link_libraries(tree-sitter-zenscript PUBLIC tree-sitter zenscript-grammar)


add_executable(tree-sitter-zenscript-test)
target_sources(tree-sitter-zenscript-test PUBLIC test/main.c)
target_link_libraries(tree-sitter-zenscript-test tree-sitter-zenscript)