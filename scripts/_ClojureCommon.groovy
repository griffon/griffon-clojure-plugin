/*
 * Copyright 2009-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Andres Almiray
 */

import griffon.util.GriffonExceptionHandler

includePluginScript('lang-bridge', '_Commons')

target(name: 'compileClojureSrc', description: "", prehook: null, posthook: null) {
    depends(compileCommons)

    def clojureSrc = "${basedir}/src/clojure"
    def clojureSrcDir = new File(clojureSrc)
    if(!clojureSrcDir.exists()) return
    if(!hasSourcesOfType(clojureSrc, ".clj")) {
        ant.echo(message: "[clojure] No Clojure sources were found.")
        return
    }

    if(sourcesUpToDate("${basedir}/src/clojure", projectMainClassesDir, ".clj")) return

    ant.echo(message: "[clojure] Compiling Clojure sources to $projectMainClassesDir")
    ant.mkdir(dir: projectMainClassesDir)
    try {
        convertNamespacePath(clojureSrc, "clojure.compile.namespaces")
        defineClojureCompilePath(clojureSrc, projectMainClassesDir)
        ant.java(classname: "clojure.lang.Compile",
                 classpathref: "clojure.compile.classpath") {
            sysproperty(key: "clojure.compile.path", value: projectMainClassesDir)
            arg(line: ant.antProject.properties."clojure.compile.namespaces")
        }
    } catch (Exception e) {
        if(argsMap.compileTrace) {
            GriffonExceptionHandler.sanitize(e)
            e.printStackTrace(System.err)
        }
        event("StatusFinal", ["Compilation error: ${e.message}"])
        exit(1)
    }
}

target(compileClojureTest: "") {
    def clojureTest = "${basedir}/test/clojure"
    def clojureTestDir = new File(clojureTest)
    if(!clojureTestDir.exists()) return
    if(!hasSourcesOfType(clojureTest, ".clj")) {
        ant.echo(message: "[clojure] No Clojure tests sources were found.")
        return
    }

    def destdir = new File(griffonSettings.testClassesDir, "clojure")
    ant.mkdir(dir: destdir)

    if(sourcesUpToDate(clojureTest, destdir.absolutePath, ".clj")) return

    ant.echo(message: "[clojure] Compiling Clojure test sources to $destdir")
    try {
        convertNamespacePath(clojureSrc, "clojure.test.namespaces")
        defineClojureTestPath(clojureTest, destdir)
        ant.java(classname: "clojure.lang.Compile",
                 classpathref: "clojure.test.classpath") {
            sysproperty(key: "clojure.compile.path", value: destdir)
            arg(line: ant.antProject.properties."clojure.test.namespaces")
        }
    } catch (Exception e) {
        if(argsMap.compileTrace) {
            GriffonExceptionHandler.sanitize(e)
            e.printStackTrace(System.err)
        }
        event("StatusFinal", ["Compilation error: ${e.message}"])
        exit(1)
    }
}

defineClojureCompilePath = { srcdir, destdir ->
    ant.path(id: "clojure.compile.classpath") {
        path(refid: "griffon.compile.classpath")
        pathElement(location: destdir)
        pathElement(location: srcdir)
    }

    if (argsMap.compileTrace) {
        println('-' * 80)
        println "[GRIFFON] 'clojure.compile.classpath' entries"
        ant.project.getReference('clojure.compile.classpath').list().each {println("  $it")}
        println('-' * 80)
    }
}

defineClojureTestPath = { srcdir, destdir ->
    ant.path(id: "clojure.test.classpath") {
        path(refid: "clojure.compile.classpath")
        pathElement(location: destdir)
        pathElement(location: srcdir)
    }

    if (argsMap.compileTrace) {
        println('-' * 80)
        println "[GRIFFON] 'clojure.test.classpath' entries"
        ant.project.getReference('clojure.test.classpath').list().each {println("  $it")}
        println('-' * 80)
    }
}

convertNamespacePath = { srcdir, pathProperty ->
    ant.pathconvert(pathsep: "", property: pathProperty) {
        fileset(dir: srcdir, includes: "**/*.clj")
        chainedmapper {
            packagemapper(from: "${srcdir}/*.clj", to: "*")
            filtermapper {
                replacestring(from: "_", to: "-")
            }
        }
    }
}
