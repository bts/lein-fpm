(ns leiningen.fpm
  (:require
    [clojure.string :as string]
    [clojure.java.io :as io]
    [clojure.java.shell :as shell]
    [me.raynes.fs :as fs]
    [leiningen.uberjar :as uberjar]))

(defn- jar-file-name
  "The filename of the standalone jar for the project."
  [project]
  (let [project-name (:name project)
        version (:version project)
        basename (string/join "-" [project-name version "standalone"])]
    (str basename ".jar")))

(defn- jar-path
  "The full path to the jar on this system."
  [project]
  (io/file (:target-path project) (jar-file-name project)))

(defn- jar-destination-path
  "The full path to the jar once installed."
  [project]
  (io/file "/usr/lib" (:name project) (jar-file-name project)))

(defn- bin-path
  "The full path to the wrapper binary on this system."
  [project]
  (io/file (:target-path project) (:name project)))

(defn- package-path
  "The full path to the output package."
  [project package-type]
  (io/file (:target-path project)
           (str (:name project) "_" (:version project) "." package-type)))

(defn- jar-invocation
  "The command to run the JAR once installed."
  [project]
  (let [jar-path (jar-destination-path project)
        opts (:jvm-opts project)
        opts-string (when opts (string/join " " opts))]
    (string/trimr (str "java -jar " jar-path " " opts-string))))

(defn wrapper-binary
  "Writes a wrapper binary to :target-path/bin and returns its path."
  [project]
  (let [binary (bin-path project)
        shebang "#!/bin/bash"
        contents (str shebang "\n" (jar-invocation project) "\n")
        _ (fs/mkdirs (fs/parent binary))
        _ (spit binary contents)
        _ (fs/chmod "+x" binary)]
    binary))

(defn- jdk-dependency
  "The JDK package name and version for the provided package type."
  [package-type]
  (condp = package-type
    "deb" "openjdk-7-jre"
    "rpm" "java-1.7.0-openjdk"
    "solaris" "jdk-7"))

(defn- options
  "The options to be passed to fpm. Returns a vector of vectors instead of a
  map to support multiple instances of the same option."
  [project package-type]
  [["-s" "dir"]
   ["-t" package-type]
   ["--force"]
   ["-a" "all"]
   ["-p" (str (package-path project package-type))]
   ["-n" (:name project)]
   ["-v" (:version project)]
   ["--url" (:url project)]
   ["--description" (:description project)]
   ["-d" (jdk-dependency package-type)]])

(defn- parameters
  "The parameters to be passed to fpm."
  [project]
  (let [project-name (:name project)
        jar-src (jar-path project)
        jar-dest (jar-destination-path project)
        bin-src (bin-path project)
        bin-dest "/usr/bin/git-server"]
    [(str jar-src "=" jar-dest)
     (str bin-src "=" bin-dest)]))

(defn package
  "Invokes fpm to build the package and returns the resulting path."
  [project package-type]
  (let [command-strings (concat ["fpm"]
                                (flatten (options project package-type))
                                (parameters project))
        ;; fpm does not seem to print to stderr:
        {:keys [exit out]} (apply shell/sh command-strings)]
    (println (string/trim-newline out))
    (when (pos? exit)
      (binding [*out* *err*]
        (println "Failed to build package!"))
      (System/exit exit))
    (package-path project package-type)))

(defn fpm
  "Generates a minimalist package for the current project."
  ([project] (fpm project "deb"))
  ([project package-type & args]
   (println "Creating uberjar")
   (uberjar/uberjar project)
   (println "Creating wrapper binary")
   (wrapper-binary project)
   (println "Building package")
   (package project package-type)))
