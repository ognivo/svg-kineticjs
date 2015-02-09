(defproject svg-kineticjs "1.0.0"
  :description "Convert svg files into KineticJS-readable JSONs (AMD-wrapped)"
  :url "https://github.com/ognivo/svg-kineticjs"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/tools.cli "0.3.1"]
                 [cheshire "5.4.0"]
                 ]
  :main svg-node.boot
  :profiles {
    :uberjar
      {:aot [svg-node.boot]
       :uberjar-name "svg-node.jar"}
  }             
)
