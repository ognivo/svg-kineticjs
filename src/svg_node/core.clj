(ns svg-node.core)
(use 'clojure.repl
     '[clojure.pprint :only [pprint]])
(require '[clojure.data.xml :as xml]
         '[cheshire.core    :as json]
         '[clojure.java.io  :as io]
         '[clojure.string   :as s])


(defn and2 [a b]
  (and a b))

(defn matches? [q i]
  (let [matches (map (fn [[k v]] (= (k i) v)) q)]
  (reduce and2 true matches)))

(defn findw [query coll]
  (first (filter #(matches? query %) coll)))

(defn read-xml-files [dir]
  (let [dir (io/file dir)
        dir-files (drop 1 (file-seq dir))]
  dir-files))

; @param style-str "fill:none;stroke:#00c430"
; @return {:fill "none" :stroke "#00c430"}
(defn parse-style [style-str]
  (let [key-value-strings (s/split style-str #";")
        keyword-value-pairs (for [str key-value-strings]
                              (let [[key value] (s/split str #":")]
                              [(keyword key) value]))]
  (reduce conj {} keyword-value-pairs)))
;(parse-style "fill:none;stroke:#00c430")

(defn get-floats [string]
  (map (comp read-string second)
       (re-seq #"([-]?[0-9]*\.?[0-9]+)" string)))
;(get-floats "0.23,-2342.2323 -900.1") # (0.23 -2342.2323 -900.1)

(defn read-n-round [num-str]
  (Math/round (double (read-string num-str))))

; @param {String} translate-str - "translate(0,-752.00213)"
; @return {hashmap} {:x 0 :y -752.00213}
; Превращает входную svg-трансформацию в данные для
; svg-трансляции. Если переданная транфсормация не translate,
; то вернёт карту с нулями, и выведет предупреждение в консоль.
(defn parse-translate-data [translate-str]
  (if (not translate-str) {:x 0 :y 0}
  (if (nil? (re-find #"translate" translate-str))
    (do (println "non-translate transform used" translate-str)
        {:x 0 :y 0})
  (let [[x y] (get-floats translate-str)]
  {:x x :y y}))))
;(parse-translate-data "translate(0,-723.23)")
;(parse-translate-data "matrix(0,-723.23)")

(def RE_m #"^m\ [-]?[0-9]*\.?[0-9]+,[-]?[0-9]*\.?[0-9]+")
(def RE_M #"^M\ [-]?[0-9]*\.?[0-9]+,[-]?[0-9]*\.?[0-9]+")
(def RE_DIGIT_PAIR #"[-]?[0-9]*\.?[0-9]+,[-]?[0-9]*\.?[0-9]+")

(defn check-m [str]
  (re-find RE_m str))
(defn check-M [str]
  (re-find RE_M str))

(def dp "m 128.89638,766.77184 c 0,0 -55.810949,68.69037 -23.99114,128.79448 67.68024,-33.08252 50.50765,-128.03687 50.50765,-128.03687 l 0,0")

(defn fmt [float-number]
  (format "%.6f" (/ float-number 1.0)))

(defn update-moved-part [moved-str x-y-data]
  (let [[x y] (get-floats moved-str)]
  (str "m " (fmt (+ x (:x x-y-data)))
       ","  (fmt (+ y (:y x-y-data)))
       )))

(defn mod-path-translate-m [path-data-str translate-data]
  (let [moved-part (re-find RE_m path-data-str)
        rest-of-str (s/replace path-data-str RE_m "")]
  (str (update-moved-part moved-part translate-data)
       rest-of-str)))

(defn mod-path-translate-M [path-data-str translate-data]
  (let [{t-x :x t-y :y} translate-data
        update-fn
          (fn [n-str]
            (let [digit-strs (s/split n-str #",")
                  [x y] (map read-string digit-strs)]
            (str (fmt (+ x t-x)) "," (fmt (+ y t-y)))))]
  (s/replace path-data-str RE_DIGIT_PAIR update-fn)))
;(mod-path-translate-M dp {:x 55 :y -68})


(defn mod-path-translate [path-data-str translate-data]
  (cond
    (check-m path-data-str)
      (mod-path-translate-m path-data-str translate-data)
    (check-M path-data-str)
      (mod-path-translate-M path-data-str translate-data)
    :else path-data-str))
;(mod-path-translate dp {:x 0, :y -766.771})

(defn xml-path->json-path 
 ([xml-path] (xml-path->json-path xml-path {:x 0 :y 0}))
 ([{:keys [attrs] :as xml-path} translate-data]
  (let [s (parse-style (:style attrs))
        translate-data-2 (parse-translate-data (attrs :transform))
        path-data (-> (:d attrs)
                      (mod-path-translate translate-data)
                      (mod-path-translate translate-data-2))]
  {:className "Path"
   :attrs
   {:data        path-data
    :strokeWidth (read-string (:stroke-width s))
    :stroke      (:stroke s)
    :lineCap     (:stroke-linecap s)}})))


; (defn filename->xml [filename]
;   (-> filename io/file io/reader xml/parse))
; (def t-xml (filename->xml "img/minifir-1.svg"))
; (def g-tag (findw {:tag :g} (:content t-xml)))


(defn svg->kinetic-json [{:keys [tag attrs content] :as svg-data}]
  (let [graphic-data (findw {:tag :g} content)
        paths (:content graphic-data)
        transform-data (:transform (:attrs graphic-data))
        translate-data (parse-translate-data transform-data)]
  {:className "Group"
   :attrs {:width  (read-n-round (:width attrs))
           :height (read-n-round (:height attrs))}
   :children (map #(xml-path->json-path % translate-data) paths)}))

;(svg->kinetic-json  

; (def svg
;   (first (read-xml-files "img/")))
; (pprint svg)
; (svg->kinetic-json svg)

(defn parse-file [file]
  (-> file io/reader xml/parse svg->kinetic-json))

(defn write-json-file [name content]
  (spit name 
        (str "define(" (json/generate-string content {:pretty true}) ")")
        ))

(defn convert-xmls->jsons [dir]
  (let [dir-files (read-xml-files dir)
        svg-files (filter #(re-find #"\.svg$" (.getName %)) dir-files)
        json-datas (map #(vector (.getPath %) (parse-file %)) svg-files)]
  (doseq [[name json] json-datas]
    (write-json-file (str name ".js") json))))

;(convert-xmls->jsons "img/")
