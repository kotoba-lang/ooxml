(ns ooxml.core
  "EDN model for OOXML OPC packages, relationships, and content types."
  (:require [clojure.string :as str]))

(def content-types-path "[Content_Types].xml")
(def root-rels-path "_rels/.rels")

(def relationship-ns "http://schemas.openxmlformats.org/package/2006/relationships")
(def content-types-ns "http://schemas.openxmlformats.org/package/2006/content-types")
(def office-document-rel "http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument")

(def content-types
  {:pptx "application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"
   :docx "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"
   :xlsx "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"
   :rels "application/vnd.openxmlformats-package.relationships+xml"
   :xml "application/xml"})

(defn xml-esc [s]
  (-> (str s)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))

(defn package
  ([entries] (package entries {}))
  ([entries opts]
   {:ooxml/entries (into (sorted-map) entries)
    :ooxml/kind (or (:kind opts) (some (fn [[prefix kind]]
                                         (when (some #(str/starts-with? % prefix) (keys entries)) kind))
                                       [["ppt/" :pptx] ["word/" :docx] ["xl/" :xlsx]]))}))

(defn entries [pkg] (:ooxml/entries pkg))
(defn kind [pkg] (:ooxml/kind pkg))

(defn rels-path-for [part-name]
  (let [i (.lastIndexOf ^String part-name "/")
        dir (if (neg? i) "" (subs part-name 0 i))
        base (if (neg? i) part-name (subs part-name (inc i)))]
    (if (seq dir)
      (str dir "/_rels/" base ".rels")
      (str "_rels/" base ".rels"))))

(defn relationship
  [{:keys [id type target target-mode]}]
  (cond-> {:id id :type type :target target}
    target-mode (assoc :target-mode target-mode)))

(defn relationship-xml [{:keys [id type target target-mode]}]
  (str "<Relationship Id=\"" (xml-esc id)
       "\" Type=\"" (xml-esc type)
       "\" Target=\"" (xml-esc target) "\""
       (when target-mode (str " TargetMode=\"" (xml-esc target-mode) "\""))
       "/>"))

(defn relationships-xml [rels]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
       "<Relationships xmlns=\"" relationship-ns "\">"
       (apply str (map relationship-xml rels))
       "</Relationships>"))

(defn default-content-type [extension content-type]
  {:kind :default :extension extension :content-type content-type})

(defn override-content-type [part-name content-type]
  {:kind :override :part-name part-name :content-type content-type})

(defn content-type-xml [{:keys [kind extension part-name content-type]}]
  (case kind
    :default (str "<Default Extension=\"" (xml-esc extension)
                  "\" ContentType=\"" (xml-esc content-type) "\"/>")
    :override (str "<Override PartName=\"" (xml-esc part-name)
                   "\" ContentType=\"" (xml-esc content-type) "\"/>")))

(defn content-types-xml [items]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
       "<Types xmlns=\"" content-types-ns "\">"
       (apply str (map content-type-xml items))
       "</Types>"))

(defn ensure-entry [pkg path value]
  (assoc-in pkg [:ooxml/entries path] value))

(defn with-root-relationship [pkg rel]
  (ensure-entry pkg root-rels-path (relationships-xml [rel])))
