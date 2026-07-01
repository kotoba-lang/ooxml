(ns ooxml.core-test
  (:require [clojure.test :refer [deftest is]]
            [ooxml.core :as ooxml]))

(deftest detects-package-kind
  (is (= :pptx (ooxml/kind (ooxml/package {"ppt/presentation.xml" "<p:presentation/>"}))))
  (is (= :docx (ooxml/kind (ooxml/package {"word/document.xml" "<w:document/>"}))))
  (is (= :xlsx (ooxml/kind (ooxml/package {"xl/workbook.xml" "<workbook/>"})))))

(deftest renders-rels-and-content-types
  (is (= "ppt/_rels/presentation.xml.rels" (ooxml/rels-path-for "ppt/presentation.xml")))
  (is (re-find #"Relationship Id=\"rId1\""
               (ooxml/relationships-xml [(ooxml/relationship {:id "rId1" :type ooxml/office-document-rel :target "ppt/presentation.xml"})])))
  (is (re-find #"\[Content_Types\]"
               (str ooxml/content-types-path))))
