;; Copyright 2020 The Defold Foundation
;; Licensed under the Defold License version 1.0 (the "License"); you may not use
;; this file except in compliance with the License.
;;
;; You may obtain a copy of the License, together with FAQs at
;; https://www.defold.com/license
;;
;; Unless required by applicable law or agreed to in writing, software distributed
;; under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
;; CONDITIONS OF ANY KIND, either express or implied. See the License for the
;; specific language governing permissions and limitations under the License.

(ns editor.spineext
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [editor.protobuf :as protobuf]
            [dynamo.graph :as g]
            [editor.build-target :as bt]
            [editor.graph-util :as gu]
            [editor.geom :as geom]
            [editor.resource :as resource]
            [editor.resource-node :as resource-node]
            [editor.validation :as validation]
            [editor.workspace :as workspace]
            [editor.types :as types]
            [editor.outline :as outline])
  (:import [editor.types AABB]
           [org.apache.commons.io IOUtils]
           [java.io IOException]
           [java.util HashSet]
           [java.net URL]
           [javax.vecmath Matrix4d Vector3d Vector4d]))

(set! *warn-on-reflection* true)

(def simpledata-icon "/defold-simpledata/editor/resources/icons/32/Icons-SimpleData.png")
(def simpledata-ext "simpledata")
(def simpledata-plugin-desc-cls (workspace/load-class! "com.dynamo.simpledata.proto.SimpleData$SimpleDataDesc"))

;;//////////////////////////////////////////////////////////////////////////////////////////////

(g/defnk produce-outline-data
  [_node-id]
  {:node-id _node-id
   :node-outline-key "SimpleData"
   :label "SimpleData"
   :icon simpledata-icon})

(defn- set-form-op [{:keys [node-id]} [property] value]
  (g/set-property! node-id property value))

(defn- clear-form-op [{:keys [node-id]} [property]]
  (g/clear-property! node-id property))


(g/defnk produce-simpledata-pb [name f32 u32 i32 u64 i64]
  {:name name
   :f32 f32
   :u32 u32
   :i32 i32
   :u64 u64
   :i64 i64})

(defn build-simpledata
  [resource dep-resources user-data]
  (let [pb-msg (reduce #(assoc %1 (first %2) (second %2))
                       (:pb-msg user-data)
                       (map (fn [[label res]] [label (resource/proj-path (get dep-resources res))]) (:dep-resources user-data)))]
    {:resource resource
     :content (protobuf/map->bytes simpledata-plugin-desc-cls pb-msg)}))

(g/defnk produce-form-data
  [_node-id name f32 u32 i32 u64 i64]
  {:navigation false
   :form-ops {:user-data {:node-id _node-id}
              :set set-form-op
              :clear clear-form-op}
   :sections [{:title "SimpleData"
               :fields [{:path [:name]
                         :label "Name"
                         :type :string}
                        {:path [:f32]
                         :label "F32"
                         :type :number}
                        {:path [:u32]
                         :label "U32"
                         :type :integer}
                        {:path [:i32]
                         :label "I32"
                         :type :integer}
                        {:path [:u64]
                         :label "U64"
                         :type :integer}
                        {:path [:i64]
                         :label "I64"
                         :type :integer}
                         ]}]
   :values {[:name] name
            [:f32] f32
            [:u32] u32
            [:i32] i32
            [:u64] u64
            [:i64] i64}})

(g/defnk produce-build-targets
  [_node-id resource dep-build-targets simpledata-pb]
  [(bt/with-content-hash
     {:node-id _node-id
      :resource (workspace/make-build-resource resource)
      :build-fn build-simpledata
      :user-data {:pb-msg simpledata-pb
                  :dep-resources []}
      :deps dep-build-targets})])

(defn load-simpledata [project self resource data]
  (g/set-property self
    :name (:name data)
    :f32 (:f32 data)
    :u32 (:u32 data)
    :i32 (:i32 data)
    :u64 (:u64 data)
    :i64 (:i64 data)))

(g/defnode SimpleDataNode
  (inherits resource-node/ResourceNode)

  (property name g/Str (default "unknown"))
  (property f32 g/Num (default 0.0)
            (dynamic error (validation/prop-error-fnk :fatal validation/prop-1-1? f32)))

  (property u32 g/Int (default 0)
            (dynamic error (validation/prop-error-fnk :fatal validation/prop-negative? u32)))
  (property i32 g/Int (default 0))

  (property u64 g/Int (default 0)
            (dynamic error (validation/prop-error-fnk :fatal validation/prop-negative? u64)))
  (property i64 g/Int (default 0))


  (input dep-build-targets g/Any :array)

  (output aabb AABB :cached (g/fnk [] geom/empty-bounding-box))

  (output form-data g/Any :cached produce-form-data)
  (output node-outline outline/OutlineData :cached produce-outline-data)

  (output simpledata-pb g/Any produce-simpledata-pb)
  (output save-value g/Any (gu/passthrough simpledata-pb))

  (output build-targets g/Any :cached produce-build-targets))

;;//////////////////////////////////////////////////////////////////////////////////////////////


(defn register-resource-types [workspace]
  (concat
   (resource-node/register-ddf-resource-type workspace
                                             :ext simpledata-ext
                                             :label "SimpleData"
                                             :node-type SimpleDataNode
                                             :ddf-type simpledata-plugin-desc-cls
                                             :load-fn load-simpledata
                                             :icon simpledata-icon
                                             :view-types [:cljfx-form-view :text]
                                             :view-opts {}
                                             :tags #{:component}
                                             ;:tag-opts {:component {:transform-properties #{:position :rotation}}}
                                             :tag-opts {:component {:transform-properties #{}}}
                                             :template "/defold-simpledata/editor/resources/templates/template.spinemodel")
   ))

; The plugin
(defn load-plugin-simpledata [workspace]
  (g/transact (concat (register-resource-types workspace))))

(defn return-plugin []
  (fn [x] (load-plugin-simpledata x)))
(return-plugin)
