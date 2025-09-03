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
            [editor.protobuf :as protobuf]
            [editor.resource-node :as resource-node]
            [editor.types :as types]
            [editor.validation :as validation]
            [editor.workspace :as workspace]))

(set! *warn-on-reflection* true)

(def ^:private simpledata-ext "simpledata")
(def ^:private simpledata-icon "/defold-simpledata/editor/resources/icons/Icon-SimpleData.png")
(def ^:private simpledata-template "/defold-simpledata/editor/resources/templates/template.simpledata")

(def ^:private simpledata-plugin-desc-cls
  (delay (workspace/load-class! "com.dynamo.simpledata.proto.SimpleData$SimpleDataDesc")))

;; /////////////////////////////////////////////////////////////////////////////////////////////

;; Helper function for property validation. Applies the supplied validate-fn to
;; the value and the property name, and returns an ErrorValue in case it returns
;; a non-nil string expressing a problem with the value.
(defn- validate-property [prop-kw validate-fn node-id value]
  (validation/prop-error :fatal node-id prop-kw validate-fn value (validation/keyword->name prop-kw)))

;; Property validate-fn for use with the validate-property function. Takes a
;; property value and the name of the property. Is expected to test the validity
;; of the property value, and return a string describing the problem in case the
;; value is not valid. For valid values, it should return nil.
(defn- prop-empty? [value prop-name]
  (when (empty? value)
    (format "'%s' must be specified" prop-name)))

;; These all validate a single property and produce a human-readable error
;; message if the value is invalid. They are used for validation in the
;; Property panel, and to produce build errors when building the project.
(defn- validate-name [node-id value]
  (validate-property :name prop-empty? node-id value))

(defn- validate-f32 [node-id value]
  (validate-property :f32 (partial validation/prop-outside-range? [0.0 100.0]) node-id value))

(defn- validate-u32 [node-id value]
  (validate-property :u32 validation/prop-negative? node-id value))

(defn- validate-u64 [node-id value]
  (validate-property :u64 validation/prop-negative? node-id value))

;; Build function embedded in the build targets for SimpleData. Once build
;; targets have been gathered, this function will be called with a BuildResource
;; (for output), and the user-data from a SimpleData build target produced by
;; the produce-build-targets defnk. It's expected to return a map containing the
;; BuildResource and the content that should be written to it as a byte array.
(defn- build-simpledata [resource _dep-resources user-data]
  (let [simpledata-pb (:simpledata-pb user-data)
        content (protobuf/map->bytes @simpledata-plugin-desc-cls simpledata-pb)]
    {:resource resource
     :content content}))

;; Produce the build targets for a single SimpleData resource. Each SimpleData
;; resource results in one binary resource for the engine runtime. The contents
;; of the build target are hashed and used to determine if we need to re-run the
;; build-fn and write a new file. If there are build errors, return an
;; ErrorValue that will abort the build and report the errors to the user.
(g/defnk produce-build-targets [_node-id resource save-value own-build-errors]
  (g/precluding-errors own-build-errors
    [(bt/with-content-hash
       {:node-id _node-id
        :resource (workspace/make-build-resource resource)
        :build-fn build-simpledata
        :user-data {:simpledata-pb save-value}})]))

;; Callback invoked by the form view when a value is edited by the user. Is
;; expected to return a sequence of transaction steps that perform the relevant
;; changes to the graph. In our case, we simply set the value of the property on
;; the edited SimpleDataNode.
(defn- set-form-op [user-data property-path value]
  (assert (= 1 (count property-path)))
  (let [node-id (:node-id user-data)
        prop-kw (first property-path)]
    (g/set-property node-id prop-kw value)))

;; Callback invoked by the form view when a value is cleared (or reset), by the
;; user. Is expected to perform the relevant changes to the graph. In our case,
;; we simply clear the value of the property on the edited SimpleDataNode.
(defn- clear-form-op [user-data property-path]
  (assert (= 1 (count property-path)))
  (let [node-id (:node-id user-data)
        prop-kw (first property-path)]
    (g/clear-property node-id prop-kw)))

;; Produce form-data for editing SimpleData using the form view. This can be
;; used to open standalone SimpleData resources in an editor tab. The form view
;; will render a simple user-interface based on the data we return here.
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

;; Produce a Clojure map representation of the protobuf field values that can be
;; saved to disk in protobuf text format, or built into a binary protobuf
;; message for the engine runtime. To keep the project files small, we omit
;; default values from the output.
(g/defnk produce-save-value [name f32 u32 i32 u64 i64 v3 array-f32]
  (protobuf/make-map-without-defaults @simpledata-plugin-desc-cls
    :name name
    :f32 f32
    :u32 u32
    :i32 i32
    :u64 u64
    :i64 i64
    :v3 v3
    :array-f32 array-f32))

;; Produce an ErrorPackage of one or more ErrorValues that express problems with
;; our SimpleData. If there are no errors, produce nil. Any errors produced here
;; will be reported as clickable errors in the Build Errors view.
(g/defnk produce-own-build-errors [_node-id name f32 u32 u64]
  (g/package-errors
    _node-id
    (validate-name _node-id name)
    (validate-f32 _node-id f32)
    (validate-u32 _node-id u32)
    (validate-u64 _node-id u64)))

;; After a SimpleDataNode has been created for our SimpleData resource, this
;; function is called with our self node-id and a Clojure map representation of
;; the protobuf data read from our resource. We're expected to return a sequence
;; of transaction steps that populate our SimpleDataNode from the protobuf data.
;; In our case, that simply means setting the property values on our node to the
;; values from the protobuf data.
(defn- load-simpledata [_project self _resource data]
  (gu/set-properties-from-pb-map self @simpledata-plugin-desc-cls data
    name :name
    f32 :f32
    u32 :u32
    i32 :i32
    u64 :u64
    i64 :i64
    v3 :v3
    array-f32 :array-f32))

;; Defines a node type that will represent SimpleData resources in the graph.
;; Whenever we encounter a .simpledata file in the project, a SimpleDataNode is
;; created for it, and the load-fn we register for the resource type will be run
;; to populate the SimpleDataNode from the protobuf data. We implement a series
;; of named outputs to make it possible to edit the node using the form view,
;; save changes, build binaries for the engine runtime, and so on.
(g/defnode SimpleDataNode
  (inherits resource-node/ResourceNode)

  ;; Editable properties.
  ;; The defaults should be equal to the ones in the SimpleData$SimpleDataDesc
  ;; class generated from `simpledata_ddf.proto`. This ensures we'll have the
  ;; correct defaults for fields not present in the `.simpledata` files.
  (property name g/Str
            (default (protobuf/default @simpledata-plugin-desc-cls :name))
            (dynamic error (g/fnk [_node-id name] (validate-name _node-id name))))

  (property f32 g/Num
            (default (protobuf/default @simpledata-plugin-desc-cls :f32))
            (dynamic error (g/fnk [_node-id f32] (validate-f32 _node-id f32))))

  (property u32 g/Int
            (default (protobuf/default @simpledata-plugin-desc-cls :u32))
            (dynamic error (g/fnk [_node-id u32] (validate-u32 _node-id u32))))

  (property i32 g/Int
            (default (protobuf/default @simpledata-plugin-desc-cls :i32)))

  (property u64 g/Int
            (default (protobuf/default @simpledata-plugin-desc-cls :u64))
            (dynamic error (g/fnk [_node-id u64] (validate-u64 _node-id u64))))

  (property i64 g/Int
            (default (protobuf/default @simpledata-plugin-desc-cls :i64)))

  (property v3 types/Vec3
            (default (protobuf/default @simpledata-plugin-desc-cls :v3)))

  (property array-f32 g/Any
            (default []))

  ;; Outputs we're expected to implement.
  (output form-data g/Any :cached produce-form-data)
  (output save-value g/Any :cached produce-save-value)
  (output own-build-errors g/Any produce-own-build-errors)
  (output build-targets g/Any :cached produce-build-targets))

;; /////////////////////////////////////////////////////////////////////////////////////////////

;; Register our .simpledata resource type with the workspace. Whenever we find a
;; .simpledata file in the project, a SimpleDataNode is created for it. Then,
;; the load-fn populates the SimpleDataNode from a Clojure map representation of
;; the protobuf data we load from the .simpledata resource. When we register our
;; resource type, we also tag ourselves as a component that can be used in game
;; objects, and declare which view types can present our resource for editing in
;; an editor tab. In our case, we will use the form view for editing. To work
;; with the form view, our node is expected to implement the form-data output
;; required by the form view.
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
    :template simpledata-template))

;; The plugin
(defn- load-plugin-simpledata [workspace]
  (g/transact (register-resource-types workspace)))

(defn- return-plugin []
  (fn [workspace]
    (load-plugin-simpledata workspace)))

(return-plugin)
