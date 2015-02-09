(ns svg-node.boot (:gen-class))
(require '[clojure.tools.cli :as cli]
         '[clojure.java.io   :as io]
         '[clojure.string    :as s]
         '[svg-node.core :as app])

(def cli-opts [
  ["-h" "--help"]
])

(def usage-str (s/join \n [
  "Usage: svg-node dir-name"
]))

(defn check-is-dir? [path]
  (.isDirectory (io/as-file path)))

(defn -main [& args]
  (let [opts (cli/parse-opts args cli-opts)
        args (:arguments opts)
        [dir] args]
  (if (check-is-dir? dir)
    (app/convert-xmls->jsons dir)
    (do
      (println usage-str)
      (System/exit (if (:help opts) 0 1)))
  )))
