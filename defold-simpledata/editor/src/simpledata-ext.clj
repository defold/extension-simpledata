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

(ns editor.defold-simpledata
  (:require [dynamo.graph :as g]
            [editor.build-target :as bt]
            [editor.graph-util :as gu]
            [editor.outline :as outline]
            [editor.protobuf :as protobuf]
            [editor.resource-node :as resource-node]
            [editor.types :as types]
            [editor.validation :as validation]
            [editor.workspace :as workspace]))

(set! *warn-on-reflection* true)

(def ^:private simpledata-icon "/defold-simpledata/editor/resources/icons/Icon-SimpleData.png")
(def ^:private simpledata-ext "simpledata")

(def ^:private simpledata-plugin-desc-cls
  (delay (workspace/load-class! "com.dynamo.simpledata.proto.SimpleData$SimpleDataDesc")))

;;//////////////////////////////////////////////////////////////////////////////////////////////

(defn- set-form-op [{:keys [node-id]} [property] value]
  (g/set-property! node-id property value))

(defn- clear-form-op [{:keys [node-id]} [property]]
  (g/clear-property! node-id property))

(defn- validate-property [node-id prop-kw validate-fn value]
  (validation/prop-error :fatal node-id prop-kw validate-fn value (validation/keyword->name prop-kw)))

(defn- validate-name [node-id value]
  (validate-property node-id :name validation/prop-empty? value))

(defn- validate-f32 [node-id value]
  (validate-property node-id :f32 validation/prop-1-1? value))

(defn- validate-u32 [node-id value]
  (validate-property node-id :u32 validation/prop-negative? value))

(defn- validate-u64 [node-id value]
  (validate-property node-id :u64 validation/prop-negative? value))

(defn- build-simpledata [resource _dep-resources user-data]
  (let [simpledata-pb (:simpledata-pb user-data)
        content (protobuf/map->bytes @simpledata-plugin-desc-cls simpledata-pb)]
    {:resource resource
     :content content}))

(g/defnk produce-build-targets [_node-id resource simpledata-pb build-errors]
  (g/precluding-errors build-errors
    [(bt/with-content-hash
       {:node-id _node-id
        :resource (workspace/make-build-resource resource)
        :build-fn build-simpledata
        :user-data {:simpledata-pb simpledata-pb}})]))

(g/defnk produce-form-data [_node-id name f32 u32 i32 u64 i64 v3]
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
                        {:path [:v3]
                         :label "V3"
                         :type :vec4}]}] ; :vec4 can edit 4 or fewer components.
   :values {[:name] name
            [:f32] f32
            [:u32] u32
            [:i32] i32
            [:u64] u64
            [:i64] i64
            [:v3] v3}})

(g/defnk produce-simpledata-pb [name f32 u32 i32 u64 i64 v3]
  {:name name
   :f32 f32
   :u32 u32
   :i32 i32
   :u64 u64
   :i64 i64
   :v3 v3})

(g/defnk produce-build-errors [_node-id name f32 u32 u64]
  (g/package-errors
    _node-id
    (validate-name _node-id name)
    (validate-f32 _node-id f32)
    (validate-u32 _node-id u32)
    (validate-u64 _node-id u64)))

(defn- load-simpledata [_project self _resource data]
  (g/set-property
    self
    :name (:name data)
    :f32 (:f32 data)
    :u32 (:u32 data)
    :i32 (:i32 data)
    :u64 (:u64 data)
    :i64 (:i64 data)
    :v3 (:v3 data)))

(g/defnk produce-node-outline [_node-id]
  {:node-id _node-id
   :node-outline-key "SimpleData"
   :label "SimpleData"
   :icon simpledata-icon})

(g/defnode SimpleDataNode
  (inherits resource-node/ResourceNode)

  ;; Editable properties.
  (property name g/Str (dynamic error (g/fnk [_node-id name] (validate-name _node-id name))))
  (property f32 g/Num (dynamic error (g/fnk [_node-id f32] (validate-f32 _node-id f32))))
  (property u32 g/Int (dynamic error (g/fnk [_node-id u32] (validate-u32 _node-id u32))))
  (property i32 g/Int)
  (property u64 g/Int (dynamic error (g/fnk [_node-id u64] (validate-u64 _node-id u64))))
  (property i64 g/Int)
  (property v3 types/Vec3)

  ;; Outputs for internal use.
  (output simpledata-pb g/Any produce-simpledata-pb)
  (output build-errors g/Any produce-build-errors)

  ;; Outputs we're expected to implement.
  (output form-data g/Any :cached produce-form-data)
  (output node-outline outline/OutlineData :cached produce-node-outline)
  (output save-value g/Any (gu/passthrough simpledata-pb))
  (output build-targets g/Any :cached produce-build-targets))

;;//////////////////////////////////////////////////////////////////////////////////////////////

(defn- register-resource-types [workspace]
  (resource-node/register-ddf-resource-type
    workspace
    :ext simpledata-ext
    :label "Simple Data"
    :node-type SimpleDataNode
    :ddf-type @simpledata-plugin-desc-cls
    :load-fn load-simpledata
    :icon simpledata-icon
    :view-types [:cljfx-form-view :text]
    :view-opts {}
    :tags #{:component}
    :tag-opts {:component {:transform-properties #{}}}
    :template "/defold-simpledata/editor/resources/templates/template.simpledata"))

;; The plugin
(defn- load-plugin-simpledata [workspace]
  (g/transact (register-resource-types workspace)))

(defn- return-plugin []
  (fn [workspace]
    (load-plugin-simpledata workspace)))

(return-plugin)
