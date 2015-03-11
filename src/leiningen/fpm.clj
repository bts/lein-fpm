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

(defn- bin-destination-path
  "The full path to the binary wrapper once installed."
  [project]
  (io/file "/usr/bin" (:name project)))

(defn- upstart-path
  "The full path to the upstart script on this system."
  [project]
  (io/file (:target-path project) (str (:name project) ".conf")))

(defn- upstart-destination-path
  "The full path to the upstart script once installed."
  [project]
  (io/file "/etc/init" (str (:name project) ".conf")))

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
        init-string (if (not (empty? opts))
                      (str "java " (string/join " " opts) " -jar")  
                      "java -jar")]
    (string/join " " [init-string jar-path "$@"])))

(defn wrapper-binary
  "Writes a wrapper binary to :target-path/bin and returns its path."
  [project]
  (let [file (bin-path project)
        shebang "#!/bin/bash"
        contents (str shebang "\n" (jar-invocation project) "\n")
        _ (fs/mkdirs (fs/parent file))
        _ (spit file contents)
        _ (fs/chmod "+x" file)]
    file))

(defn upstart-script
  "Writes an upstart script and returns its path."
  [project]
  (let [file (upstart-path project)
        project-name (:name project)
        contents (str "#!upstart\n\n"
                      "description \"" project-name "\"\n"
                      "start on startup\n"
                      "stop on shutdown\n"
                      "respawn\n"
                      "exec /usr/bin/" project-name "\n")
        _ (fs/mkdirs (fs/parent file))
        _ (spit file contents)]
    file))

(defn- jdk-dependency
  "The JDK package name and version for the provided package type."
  [package-type]
  (condp = package-type
    "deb" "openjdk-7-jre-headless"
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
   ["-d" (jdk-dependency package-type)]
   ["--rpm-os" "linux"]])

(defn- parameters
  "The parameters to be passed to fpm."
  [project]
  (let [jar-src (jar-path project)
        jar-dest (jar-destination-path project)
        bin-src (bin-path project)
        bin-dest (bin-destination-path project)
        upstart-src (upstart-path project)
        upstart-dest (upstart-destination-path project)]
    [(str jar-src "=" jar-dest)
     (str bin-src "=" bin-dest)
     (str upstart-src "=" upstart-dest)]))

(defn- warnln
  "Prints a message to stderr."
  [message]
  (binding [*out* *err*]
    (println message)))

(defn package
  "Invokes fpm to build the package and returns the resulting path."
  [project package-type]
  (let [command-strings (concat ["fpm"]
                                (flatten (options project package-type))
                                (parameters project))
        {:keys [exit out err]} (apply shell/sh command-strings)]
    (when-not (empty? out)
      (println (string/trim-newline out)))
    (when-not (empty? err)
      (warnln (string/trim-newline err)))
    (when (pos? exit)
      (warnln "Failed to build package!")
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
   (println "Creating upstart script")
   (upstart-script project)
   (println "Building package")
   (package project package-type)))
