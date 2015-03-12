(ns leiningen.test.fpm
  (:require [leiningen.fpm :as fpm]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [clojure.test :refer :all]))

(def ^:private tmp-dir
  (str (fs/temp-dir "test")))

(def ^:private project
  {:name "project-name"
   :version "1.0.1"
   :url "http://project.com"
   :description "An project"
   :target-path tmp-dir})

(def ^:private jar-file-name "project-name-1.0.1-standalone.jar")

(deftest test-wrapper-binary
  (let [binary-contents #(slurp (fpm/wrapper-binary %))
        expected-jar-path (str "/usr/lib/project-name/" jar-file-name)]
    (testing "without jvm options"
      (is (= (binary-contents project)
             (str "#!/bin/bash\njava -jar " expected-jar-path " $@\n"))))
    (testing "with jvm options"
      (is (= (binary-contents
               (assoc project :jvm-opts ["-Xms256M" "-Xmx512M"]))
             (str "#!/bin/bash\njava -Xms256M -Xmx512M"
                  " -jar " expected-jar-path " $@\n"))))))

(deftest test-upstart-script
  (let [contents (slurp (fpm/upstart-script project))
        project-name (:name project)]
    (is (= contents "#!upstart

description \"project-name\"
start on startup
stop on shutdown
respawn
exec /usr/bin/project-name\n"))))

(deftest test-package
  (let [target-path (:target-path project)
        _ (fpm/wrapper-binary project)
        _ (fpm/upstart-script project)
        _ (fs/touch (io/file target-path jar-file-name))
        package (fpm/package project "deb")]
    (is (fs/exists? package))
    (is (= (.getParent package) target-path))))
