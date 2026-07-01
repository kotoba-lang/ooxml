(ns ooxml.core-test
  (:require [clojure.test :refer [deftest is]]
            [ooxml.core :as ooxml]))

(deftest detects-package-kind
  (is (= :pptx (ooxml/kind (ooxml/package {"ppt/presentation.xml" "<p:presentation/>"}))))
  (is (= :docx (ooxml/kind (ooxml/package {"word/document.xml" "<w:document/>"}))))
  (is (= :xlsx (ooxml/kind (ooxml/package {"xl/workbook.xml" "<workbook/>"}))))
  (is (= :opc (ooxml/kind (ooxml/package {"custom/item.xml" "<x/>"})))))

(deftest renders-rels-and-content-types
  (is (= "ppt/_rels/presentation.xml.rels" (ooxml/rels-path-for "ppt/presentation.xml")))
  (is (re-find #"Relationship Id=\"rId1\""
               (ooxml/relationships-xml [(ooxml/relationship {:id "rId1" :type ooxml/office-document-rel :target "ppt/presentation.xml"})])))
  (is (re-find #"\[Content_Types\]"
               (str ooxml/content-types-path))))

(deftest sorts-office-parts-naturally
  (is (= ["ppt/slides/slide1.xml" "ppt/slides/slide2.xml" "ppt/slides/slide10.xml"]
         (mapv first
               (ooxml/office-parts {"ppt/slides/slide10.xml" ""
                                     "ppt/slides/slide2.xml" ""
                                     "ppt/slides/slide1.xml" ""
                                     "ppt/theme/theme1.xml" ""})))))

(deftest appends-content-types-and-root-relationships
  (is (re-find #"Default Extension=\"edn\""
               (ooxml/ensure-content-type-extension "" "edn" "application/edn")))
  (is (= "<Types><Default Extension=\"edn\" ContentType=\"application/edn\"/></Types>"
         (ooxml/ensure-content-type-extension
          "<Types><Default Extension=\"edn\" ContentType=\"application/edn\"/></Types>"
          "edn"
          "application/edn")))
  (is (re-find #"rIdKotobaOffice"
               (ooxml/ensure-root-relationship
                "<Relationships/>"
                (ooxml/relationship {:id "rIdKotobaOffice"
                                     :type "https://kotoba-lang.org/office/relationship/causal-edn"
                                     :target "ocz/causal.edn"})))))
