(ns untangled.components.ui.forms-spec
  (:require
    [om.next :as om :refer [defui]]
    [untangled-spec.core :refer-macros [behavior specification assertions component when-mocking provided]]
    [untangled.client.core :as uc]
    [untangled.client.logging :as log]
    [untangled.client.mutations :as m]
    [untangled.components.ui.forms :as f]))

(specification "Form Elements Declarations"
  (component "subform-element"
    (when-mocking
      (log/error msg a b c d) => (do (assertions "Logs a message for joins on non-augmented classes"
                                       msg =fn=> (fn [m] (re-matches #"^Subform elem.*$" m))))
      (let [f (f/subform-element :name 'Cls)]
        (assertions
          "defaults as a to-one relation"
          (:input/cardinality f) => :one
          "is marked as a subform"
          f =fn=> f/is-subform?
          "has the correct type"
          (:input/type f) => ::f/subform
          "tracks the class of the subform UI"
          (-> f meta :component) => 'Cls))))
  (component "form-switcher-input"
    (when-mocking
      (log/error msg a b c d) => (do (assertions "Logs a message for joins on non-augmented classes"
                                       msg =fn=> (fn [m] (re-matches #"^Subform elem.*$" m))))
      (let [f (f/form-switcher-input :name 'Cls :k)]
        (assertions
          "defaults as a to-many relation"
          (:input/cardinality f) => :many
          "is marked as a subform"
          f =fn=> f/is-subform?
          "has a subform selection key"
          (:input/select-key f) => :k
          "has the correct type"
          (:input/type f) => ::f/switcher
          "tracks the class of the subform UI"
          (-> f meta :component) => 'Cls))))
  (component "id-field"
    (let [field (f/id-field :name)]
      (assertions
        "marks the field on the entity that represents the PK"
        (:input/name field) => :name
        "has the correct type"
        (:input/type field) => ::f/identity)))
  (component "text-input"
    (let [field (f/text-input :name :default-value "abc" :placeholder "Name" :className "fg" :validator 'valid? :validator-args {:v 1})]
      (assertions
        "has a name"
        (:input/name field) => :name
        "has a placeholder"
        (:input/placeholder field) => "Name"
        "supports a default value"
        (:input/default-value field) => "abc"
        "has the correct type"
        (:input/type field) => ::f/text
        "can have a validator"
        (:input/validator field) => 'valid?
        "supports a CSS class"
        (:input/css-class field) => "fg"
        "supports validator args"
        (:input/validator-args field) => {:v 1}))
    (let [field (f/text-input :name)]
      (assertions
        "placeholder defaults to empty string"
        (:input/placeholder field) => ""
        "default value defaults to empty string"
        (:input/default-value field) => ""
        "CSS class defaults to nothing"
        (:input/css-class field) => "")))
  (component "integer-input"
    (let [field (f/integer-input :age :default-value 55 :className "fg" :validator 'valid? :validator-args {:v 1})]
      (assertions
        "supports a default value"
        (:input/default-value field) => 55
        "supports a CSS class"
        (:input/css-class field) => "fg"
        "has a name"
        (:input/name field) => :age
        "has the correct type"
        (:input/type field) => ::f/integer
        "can have a validator"
        (:input/validator field) => 'valid?
        "supports validator args"
        (:input/validator-args field) => {:v 1}))
    (let [field (f/integer-input :age)]
      (assertions
        "default value defaults to 0"
        (:input/default-value field) => 0
        "CSS class defaults to empty string"
        (:input/css-class field) => "")))
  (component "checkbox-input"
    (let [field (f/checkbox-input :name)
          field-1 (f/checkbox-input :name :default-value true)]
      (assertions
        "has a name"
        (:input/name field) => :name
        "defaults to false"
        (false? (:input/default-value field)) => true
        "can be set to default to true"
        (true? (:input/default-value field-1)) => true
        "has the correct type"
        (:input/type field) => ::f/checkbox)))
  (component "dropdown-input"
    (let [field (f/dropdown-input :name [(f/option :a "A") (f/option :b "B")])]
      (assertions
        "has a name"
        (:input/name field) => :name
        "CSS class defaults to empty string"
        (:input/css-class field) => ""
        "Requires a list of options"
        (f/dropdown-input :name []) =throws=> (js/Error.)
        (f/dropdown-input :name [{:a 1}]) =throws=> (js/Error.)
        "Defaults to 'unselected'"
        (:input/default-value field) => ::f/none
        "Has the correct type"
        (:input/type field) => ::f/dropdown))
    (let [field (f/dropdown-input :name [(f/option :a "A") (f/option :b "B")]
                                  :default-value :a
                                  :className "fld")]
      (assertions
        "CSS class defaults to empty string"
        (:input/css-class field) => "fld"
        "Can default to one of the options"
        (:input/default-value field) => :a
        "Throws an exception if the default isn't defined in the option list"
        (f/dropdown-input :name [(f/option :a "A")] :default-value :x) =throws=> (js/Error.)))))

(specification "Building a Form"
  (component "The default state of a form"
    (component "id fields"
      (let [fields [(f/id-field :db/id)]
            default-state (f/default-state fields)]
        (assertions
          "are marked as valid to start"
          (-> default-state :validation :db/id) => :valid
          "are given an Om tempid by default"
          (-> default-state :state :db/id) =fn=> om/tempid?)))
    (component "non-id fields"
      (let [fields [(f/text-input :name :default-value :ABC)]
            default-state (f/default-state fields)]
        (assertions
          "are marked as validation :unchecked to start"
          (-> default-state :validation :name) => :unchecked
          "are given a default value defined by the field type"
          (-> default-state :state :name) => :ABC))))
  (component "The initialized state of a form"
    (let [entity {:a 1}
          field-keys [:a :b]
          default-state {:a 0 :b "X"}
          initial-state (f/initialized-state default-state [:a :b] entity)]
      (assertions
        "overwrites the defaults with the entity state being augmented"
        (-> initial-state :a) => 1
        "leaves defaults alone when the entity state does not contain them"
        (-> initial-state :b) => "X"
        "tolerates nil and empty entities"
        (f/initialized-state default-state field-keys {}) => default-state
        (f/initialized-state default-state field-keys nil) => default-state))))

(defui Phone
  static om/IQuery
  (query [this] [:db/id :phone/number])
  static om/Ident
  (ident [this props] [:phone/by-id (:db/id props)])
  static f/IForm
  (form-elements [this] [(f/text-input :phone/number)]))

(defui Person
  static om/IQuery
  (query [this] [:db/id :person/name {:person/number (om/get-query Phone)}])
  static om/Ident
  (ident [this props] [:people/by-id (:db/id props)])
  static f/IForm
  (form-elements [this] [(f/subform-element :person/number Phone :one)
                         (f/text-input :person/name :className "name-class")]))

(def person-db {:phone/by-id  {1 {:db/id 1 :phone/number "555-1212"}
                               2 {:db/id 2 :phone/number "555-3345"}}
                :people/by-id {3 {:db/id 3 :person/name "B"}
                               7 {:db/id 7 :person/name "A" :person/number [:phone/by-id 1]}
                               5 {:db/id 5 :person/name "D" :person/number []}
                               4 {:db/id 4 :person/name "C" :person/number [[:phone/by-id 1] [:phone/by-id 2]]}
                               6 {:db/id 6 :person/name "E" :person/number [[:phone/by-id 1]]}}})

(specification "Initializing a to-one form relation"
  (let [app-state person-db
        base-form (get-in app-state [:people/by-id 7])
        spec (-> (f/form-elements Person) first)]
    (when-mocking
      (f/init-form* state class ident v) =1x=> (do
                                                 (assertions
                                                   "initializes the proper target form"
                                                   ident => [:phone/by-id 1]
                                                   "finds the proper class of the form"
                                                   class => Phone)
                                                 :new-state)
      (let [actual (f/init-one app-state base-form spec {})]
        (assertions
          "returns the value of a call to init-form"
          actual => :new-state)))
    (assertions
      "Is ok when the target is nil"
      (f/init-one app-state (get-in app-state [:people/by-id 3]) spec {}) => app-state
      "Throws an error when targeting a to-many"
      (f/init-one app-state (get-in app-state [:people/by-id 4]) spec {}) =throws=> (js/Error.))))

(defui PolyPerson
  static om/IQuery
  (query [this] [:db/id :person/name {:person/number (om/get-query Phone)}])
  static om/Ident
  (ident [this props] [:people/by-id (:db/id props)])
  static f/IForm
  (form-elements [this] [(f/subform-element :person/number Phone :many)]))

(specification "Initializing a to-many form relation"
  (let [base-form (get-in person-db [:people/by-id 4])
        spec (-> (f/form-elements PolyPerson) first)]
    (when-mocking
      (f/init-form* state class ident v) =1x=> (do
                                                 (assertions
                                                   "initializes the first item"
                                                   ident => [:phone/by-id 1]
                                                   "finds the proper class of the form"
                                                   class => Phone)
                                                 state)
      (f/init-form* state class ident v) =1x=> (do
                                                 (assertions
                                                   "initializes the other item(s)"
                                                   ident => [:phone/by-id 2]
                                                   "finds the proper class of the form"
                                                   class => Phone)
                                                 :new-state)
      (let [actual (f/init-many person-db base-form spec {})]
        (assertions
          "returns the state of the final init-state"
          actual => :new-state)))
    (assertions
      "Is ok when the target field is nil"
      (f/init-many person-db (get-in person-db [:people/by-id 3]) spec {}) => person-db
      "Throws an error when targeting a to-one"
      (f/init-many person-db (get-in person-db [:people/by-id 7]) spec {}) =throws=> (js/Error.))))

(specification "Initializing a form recursively"
  (assertions
    "detects initialized forms by looking for form state"
    (f/initialized? {:db/id 1}) => false
    (f/initialized? {:db/id 1 :ui/form {}}) => true)
  (provided "when the form is already initialized"
    (f/initialized? f) => true

    (assertions
      "Just returns the unmodified app state"
      (f/init-form person-db Person [:people/by-id 7]) => person-db))
  (provided "when the form is partially initialized (to-one)"
    (f/initialized? f) => (boolean (:person/name f))

    (let [result (f/init-form person-db Person [:people/by-id 7])]
      (assertions
        "still initializes the nested form"
        (f/form-ident (get-in result [:phone/by-id 1])) => [:phone/by-id 1])))
  (provided "when the form is partially initialized (to-many)"
    (f/initialized? f) => (= 4 (:db/id f))

    (let [result (f/init-form person-db PolyPerson [:people/by-id 4])]
      (assertions
        "still initializes the nested form"
        (f/form-ident (get-in result [:phone/by-id 1])) => [:phone/by-id 1]
        (f/form-ident (get-in result [:phone/by-id 2])) => [:phone/by-id 2])))
  (let [actual (f/init-form person-db Person [:people/by-id 7])]
    (assertions
      "properly initializes a nested to-one form (integration)"
      (f/form-ident (get-in actual [:people/by-id 7])) => [:people/by-id 7]
      (f/form-ident (get-in actual [:phone/by-id 1])) => [:phone/by-id 1]))
  (let [actual (f/init-form person-db PolyPerson [:people/by-id 4])]
    (assertions
      "properly initializes a nested to-many form (integration)"
      (f/form-ident (get-in actual [:people/by-id 4])) => [:people/by-id 4]
      (f/form-ident (get-in actual [:phone/by-id 1])) => [:phone/by-id 1]
      (f/form-ident (get-in actual [:phone/by-id 2])) => [:phone/by-id 2])))

(defui LeafForm
  static uc/InitialAppState
  (initial-state [this {:keys [id]}] {:id id :value 1 :leaf true})
  static om/IQuery
  (query [this] [:id :leaf])
  static om/Ident
  (ident [this props] [:leaf (:id props)])
  static f/IForm
  (form-elements [this] [(f/id-field :id)]))

(defui NonForm
  static om/IQuery
  (query [this] [:id :data])
  static om/Ident
  (ident [this props] [:data (:id props)]))

(defui ToManyForm
  static uc/InitialAppState
  (initial-state [this params] {:id      99
                                :to-many true
                                :value   1
                                :leaves  [(uc/initial-state LeafForm {:id 3}) (uc/initial-state LeafForm {:id 4})]})
  static om/IQuery
  (query [this] [{:leaves (om/get-query LeafForm)} :value])
  static om/Ident
  (ident [this props] [:tomform (:id props)])
  static f/IForm
  (form-elements [this] [(f/subform-element :leaves LeafForm :many)]))

(defui Level2Form
  static uc/InitialAppState
  (initial-state [this params] {:id     2
                                :level2 true
                                :value  1
                                :leaf1  (uc/initial-state LeafForm {:id 5})
                                :leaf2  (uc/initial-state LeafForm {:id 6})})
  static om/IQuery
  (query [this] [{:leaf1 (om/get-query LeafForm)}
                 {:non-form (om/get-query NonForm)}
                 {:leaf2 (om/get-query LeafForm)}
                 {:leaf3 (om/get-query LeafForm)}])
  static om/Ident
  (ident [this props] [:level2 (:id props)])
  static f/IForm
  (form-elements [this] [(f/subform-element :leaf1 LeafForm)
                         (f/subform-element :leaf2 LeafForm)
                         (f/subform-element :leaf3 LeafForm)]))

(defui OtherRootForm
  static uc/InitialAppState
  (initial-state [this {:keys [id]}] {:id id :value 50 :other true})
  static om/IQuery
  (query [this] [:id :other])
  static om/Ident
  (ident [this props] [:other (:id props)])
  static f/IForm
  (form-elements [this] []))

(defui Level3Form
  static uc/InitialAppState
  (initial-state [this params] {:id         1
                                :level3     true
                                :other-root (uc/initial-state OtherRootForm {:id 100})
                                :level2     (uc/initial-state Level2Form {})})
  static om/IQuery
  (query [this] [{:other-root (om/get-query OtherRootForm)}
                 {:level2 (om/get-query Level2Form)}])
  static om/Ident
  (ident [this props] [:level3 (:id props)])
  static f/IForm
  (form-elements [this] [(f/subform-element :level2 Level2Form)]))

(specification "subforms*"
  (assertions
    "Are empty on non-nested forms"
    (f/subforms* LeafForm) => []
    "gives back pairs of [prop-path class] on a singly-nested form"
    (f/subforms* Level2Form) => [[[:leaf1] LeafForm] [[:leaf2] LeafForm] [[:leaf3] LeafForm]]
    "gives back pairs of [prop-path class] on a nested form"
    (f/subforms* Level3Form) => [[[:level2] Level2Form] [[:level2 :leaf1] LeafForm]
                                 [[:level2 :leaf2] LeafForm] [[:level2 :leaf3] LeafForm]]
    "supports to-many sub-forms"
    (f/subforms* ToManyForm) => [[[:leaves] LeafForm]]))

(def nested-form-db (om/tree->db [{:root (om/get-query Level3Form)}]
                      {:root (uc/initial-state Level3Form {})}, true))
(def tomany-form-db (om/tree->db [{:root (om/get-query ToManyForm)}]
                      {:root (uc/initial-state ToManyForm {})}, true))

(specification "to-idents"
  (assertions
    "finds a sequence of idents by walking the graph (to-one)"
    (f/to-idents person-db (get-in person-db [:people/by-id 7]) [:person/number]) => [[:phone/by-id 1]]
    "finds a sequence of idents by walking the graph (to-many)"
    (f/to-idents tomany-form-db (get-in tomany-form-db [:tomform 99]) [:leaves]) => [[:leaf 3] [:leaf 4]]))

(specification "get-forms"
  (assertions
    "Obtains a list of nested forms and idents that are BOTH declared as subforms AND are present (others are skipped)."
    (f/get-forms nested-form-db Level3Form [:level3 1])
    => [{:ident [:level3 1] :class Level3Form
         :form {:id 1 :level3 true :other-root [:other 100] :level2 [:level2 2]}}
        {:ident [:level2 2] :class Level2Form
         :form {:id 2 :value 1 :level2 true :leaf1 [:leaf 5] :leaf2 [:leaf 6]}}
        {:ident [:leaf 5] :class LeafForm :form {:id 5 :value 1 :leaf true}}
        {:ident [:leaf 6] :class LeafForm :form {:id 6 :value 1 :leaf true}}]
    "Can pull to-many relations of sub-forms"
    (f/get-forms tomany-form-db ToManyForm [:tomform 99])
    => [{:ident [:tomform 99]
         :class ToManyForm
         :form  {:id 99 :to-many true :value 1 :leaves [[:leaf 3] [:leaf 4]]}}
        {:ident [:leaf 3]
         :class LeafForm
         :form  {:id 3 :value 1 :leaf true}}
        {:ident [:leaf 4]
         :class LeafForm
         :form  {:id 4 :value 1 :leaf true}}]))

(specification "update-forms"
  (let [state (f/init-form nested-form-db Level3Form [:level3 1])
        form (get-in state [:level3 1])
        new-state (f/update-forms state form (fn [{:keys [form]}] (assoc form :touched true)))]
    (assertions
      "Updates the state of the correct forms using a supplied function."
      (get-in new-state [:level3 1 :touched]) => true
      (get-in new-state [:level2 2 :touched]) => true
      (get-in new-state [:leaf 5 :touched]) => true
      (get-in new-state [:leaf 6 :touched]) => true
      (get-in new-state [:other 100 :touched]) => nil)))

(specification "reduce-forms"
  (assertions "Can accumulate values from all forms"
    (let [state (f/init-form nested-form-db Level3Form [:level3 1])
          form (get-in state [:level3 1])]
      (f/reduce-forms state form (fn [acc spec] (+ acc (get-in spec [:form :value]))) 0)) => 3))

(specification "Form config and state helpers"
  (let [ident-under-test [:people/by-id 7]
        app-state (f/init-form person-db Person ident-under-test)
        person-form (get-in app-state ident-under-test)]
    (assertions
      "Can access the component for the form"
      (f/form-component person-form) => Person
      "Can access the ident of the form's location in the state"
      (f/form-ident person-form) => ident-under-test
      "Can access the config of a given field on a form"
      (get (f/field-config person-form :person/number) :input/name) => :person/number
      "Can access the type of a given field on a form"
      (f/field-type person-form :person/number) => ::f/subform
      "Can identify a subform field"
      (f/is-subform? person-form :person/number) => true
      (f/is-subform? {:ui/form {:elements/by-name {:person/mate {:input/is-form? true}}}} :person/mate) => true
      "Can access the current (edited) value"
      (f/current-value person-form :person/name) => "A"
      "Can modify the current value"
      (f/current-value person-form :person/name) =fn=> (comp not #{"QQ" "JJ"})
      (-> person-form
        (f/update-current-value :person/name (constantly "QQ"))
        (f/current-value :person/name))
      => "QQ"
      (-> person-form
        (f/set-current-value :person/name "JJ")
        (f/current-value :person/name))
      => "JJ"
      "Can access the desired CSS class of a field"
      (f/css-class person-form :person/name) => "name-class"
      "Can access field names for all validatable fields"
      (f/validatable-fields person-form) => [:person/name])))

(defn test-mutate [env disp params]
  ((:action (m/mutate env disp params))))

(defui Thing
  static f/IForm
  (form-elements [this]
    [(f/id-field :db/id)
     (f/text-input :thing/name)
     (f/checkbox-input :thing/ok?)])
  static om/IQuery
  (query [this] [:db/id :thing/name :thing/ok?])
  static om/Ident
  (ident [this props] [:thing/by-id (:db/id props)]))

(specification "Form state mutations"
  (let [db (-> {:thing/by-id {1 {:db/id 1}}}
             (f/init-form Thing [:thing/by-id 1]))
        test-mutate-field (partial test-mutate {:state (atom db)})

        thing-1 (get-in db [:thing/by-id 1])]
    (component "set-field"
      (assertions
        (f/current-value thing-1 :thing/name) =fn=> #(not= % "asdf")
        (-> (test-mutate-field `f/set-field
              {:form-id [:thing/by-id 1] :field :thing/name :value "asdf"})
          (get-in [:thing/by-id 1])
          (f/current-value :thing/name))
        => "asdf"))
    (component "toggle-field"
      (assertions
        (f/current-value thing-1 :thing/ok?) =fn=> not
        (-> (test-mutate-field `f/toggle-field
              {:form-id [:thing/by-id 1] :field :thing/ok?})
          (get-in [:thing/by-id 1])
          (f/current-value :thing/ok?))
        => true))))

(defmethod f/form-field-valid? 'is-named? [sym v {:keys [name]}] (= v name))

(defui CPerson ; only valid if name is 'C'
  static om/IQuery
  (query [this] [:db/id :person/name {:person/number (om/get-query Phone)}])
  static om/Ident
  (ident [this props] [:people/by-id (:db/id props)])
  static f/IForm
  (form-elements [this] [(f/text-input :person/name :className "name-class" :validator 'is-named? :validator-args {:name "C"})
                         (f/subform-element :person/number Phone :many)]))

(specification "Form Validation"
  (let [app-state (-> person-db
                      (f/init-form CPerson [:people/by-id 4])
                      (f/init-form CPerson [:people/by-id 5]))
        unchecked-person (get-in app-state [:people/by-id 5])
        c-person (get-in app-state [:people/by-id 4])
        valid-person (f/validate-field c-person :person/name)
        invalid-person (f/validate-field unchecked-person :person/name)
        validated-person (f/validate-fields unchecked-person)]
    (component "Update validation (on a field)"
      (assertions
        "properly marks valid using the provided validator (integration)"
        (f/current-validity valid-person :person/name) => :valid
        "properly marks invalid using the provided validator (integration)"
        (f/current-validity invalid-person :person/name) => :invalid))
    (component "validate-fields"
      (assertions
        "non-recursively updates validation on all fields"
        (f/valid? validated-person :person/name) => false))
    (assertions
      "Can query the tri-state validity of a specific field (which defaults to unchecked)"
      (f/current-validity unchecked-person :person/name) => :unchecked
      "indicates a field is invalid iff it is marked invalid (integration)"
      (f/invalid? unchecked-person :person/name) => false
      (f/invalid? valid-person :person/name) => false
      (f/invalid? invalid-person :person/name) => true
      "indicates a field is valid iff it is marked valid (integration)"
      (f/valid? unchecked-person :person/name) => false
      (f/valid? valid-person :person/name) => true
      (f/valid? invalid-person :person/name) => false
      "Can find the validation trigger symbol for a field"
      (f/validator invalid-person :person/name) => 'is-named?
      "Can find the validation trigger args for a field"
      (f/validator-args invalid-person :person/name) => {:name "C"}
      "Can see if a field is clean on the form"
      (f/dirty? valid-person) => false
      "Can see if a field is dirty on the form"
      (f/dirty? (assoc-in valid-person [:person/name] "X")) => true
      "Can recursively check for dirty data on a form"
      (f/any-dirty? app-state c-person) => false
      (f/any-dirty? (assoc-in app-state [:people/by-id 4 :person/name] "X") c-person) => true
      (f/any-dirty? (assoc-in app-state [:phone/by-id 1 :phone/number] "222") c-person) => true
      (f/any-dirty? (assoc-in app-state [:phone/by-id 2 :phone/number] "4") c-person) => true))

  (component "validate-forms"
    (let [app-state (f/init-form person-db CPerson [:people/by-id 4])
          validated-state (f/validate-forms app-state [:people/by-id 4])
          get-person (fn [id] (get-in validated-state [:people/by-id id]))
          get-phone (fn [id] (get-in validated-state [:phone/by-id id]))]
      (assertions
        "recursively walks an entire form in app state and updates all fields validity to proper valid/invalid."
        (f/valid? (get-person 4) :person/name) => true
        (f/invalid? (get-person 4) :person/name) => false
        (f/valid? (get-phone 1) :phone/number) => true
        (f/invalid? (get-phone 1) :phone/number) => false
        (f/valid? (get-phone 2) :phone/number) => true
        (f/invalid? (get-phone 2) :phone/number) => false))))

(defn fix-tx "hack/fix for github.com/untangled-web/untangled-spec/issues/6"
  [tx] (mapcat #(if (seq? %) (vec %) [%]) tx))

(let [app-state (-> person-db
                  (f/init-form Phone [:phone/by-id 1])
                  (f/init-form Phone [:phone/by-id 2])
                  (f/init-form Person [:people/by-id 7])
                  (f/init-form Person [:people/by-id 3])
                  (f/init-form PolyPerson [:people/by-id 4])
                  (f/init-form PolyPerson [:people/by-id 5])
                  (f/init-form PolyPerson [:people/by-id 6]))
      basic-person (get-in app-state [:people/by-id 3])
      one-number-person (get-in app-state [:people/by-id 7])
      no-number-person (get-in app-state [:people/by-id 5])
      many-number-person (get-in app-state [:people/by-id 4])
      one-many-number-person (get-in app-state [:people/by-id 6])

      test-diff-form
      (fn [form f path & args]
        (-> app-state
          (#(apply f % (concat (f/form-ident form) path) args))
          (f/diff-form form)))]
  (specification "Form entity commit/reset"
    (component "commit-to-entity"
      (component "diff-form"
        (assertions
          (f/diff-form app-state basic-person) => {}
          (test-diff-form basic-person assoc-in [:person/name] "Foo Bar")
          => {(f/form-ident basic-person) {:person/name {:new "Foo Bar" :old "B"}}}
          (test-diff-form one-number-person
            update-in [] dissoc :person/name)
          => {(f/form-ident one-number-person) {:person/name {:old "A"}}}
          "we can modify & pick up changes to entities we reference"
          (-> app-state
            (assoc-in [:phone/by-id 1 :phone/number] "123-4567")
            (f/diff-form one-number-person))
          => {[:phone/by-id 1] {:phone/number {:old "555-1212" :new "123-4567"}}}
          (-> app-state
            (assoc-in [:phone/by-id 1 :phone/number] "123-4567")
            (f/diff-form many-number-person))
          => {[:phone/by-id 1] {:phone/number {:new "123-4567" :old "555-1212"}}}
          ;;*only* those we reference
          (-> app-state
            (assoc-in [:phone/by-id 1 :phone/number] "123-4567")
            (f/diff-form no-number-person))
          => {}
          "we send a minimal delta for modification of refs (ie idents)"
          (test-diff-form no-number-person
            assoc-in [:phone/by-id 1 :phone/number] "123-4567")
          => {}
          ;; new ref one
          (test-diff-form basic-person
            assoc-in [:person/number] [:phone/by-id 1])
          => {(f/form-ident basic-person) {:person/number {:new [:phone/by-id 1]}}}
          ;; add & del ref one
          (test-diff-form one-number-person
            assoc-in [:person/number] [:phone/by-id 2])
          => {(f/form-ident one-number-person) {:person/number {:new [:phone/by-id 2], :old [:phone/by-id 1]}}}
          ;;del ref many
          (test-diff-form one-many-number-person
            assoc-in [:person/number] [])
          => {(f/form-ident one-many-number-person) {:person/number {:del [[:phone/by-id 1]]}}}
          ;; add ref many
          (test-diff-form no-number-person
            update-in [:person/number] conj [:phone/by-id 1])
          => {(f/form-ident no-number-person) {:person/number {:add [[:phone/by-id 1]]}}}
          (test-diff-form many-number-person
            update-in [:person/number] conj [:phone/by-id 3])
          => {(f/form-ident many-number-person) {:person/number {:add [[:phone/by-id 3]]}}}
          ;; del & add ref many
          (test-diff-form many-number-person
            assoc-in [:person/number 0] [:phone/by-id 3])
          => {(f/form-ident many-number-person) {:person/number {:add [[:phone/by-id 3]], :del [[:phone/by-id 1]]}}}))
      (when-mocking
        (f/entity-x-form _ form-id xf) => (do (assertions
                                                form-id => [:people/by-id 3]
                                                xf => f/commit-state)
                                            ::ok)
        (f/diff-form _ _) => :fake/delta
        (let [commit-mut
              (m/mutate {:state (atom app-state)
                         :ast {:params {:remote true}}}
                `f/commit-to-entity
                {:remote true
                 :form-id (f/form-ident basic-person)})]
          (assertions
            "only optionally `:remote`s the result of diff-form to the server"
            (:remote commit-mut) => {:params {:delta :fake/delta}}
            "the optimistic action commits the current value of the form to be its new :original state"
            ((:action commit-mut)) => ::ok)))
      (component "commit-to-entity! - api/public function"
        (when-mocking
          (om/props :fake/component) => :fake/props
          (om/transact! :fake/component tx) => (fix-tx tx)
          (f/form-ident _) => :fake/form-ident
          (behavior "only commits if form is valid after validating"
            (when-mocking
              (f/valid? :fake/props) => true
              (assertions
                (f/commit-to-entity! :fake/component)
                => `[f/commit-to-entity {:form-id :fake/form-ident :remote false}
                     :ui/form-root]))
            (when-mocking
              (f/valid? :fake/props) => false
              (assertions
                (f/commit-to-entity! :fake/component)
                => `[f/validate-form {:form-id :fake/form-ident}
                     :ui/form-root])))
          (behavior "optional `:rerender` key"
            (assertions
              (last (f/commit-to-entity! :fake/component :rerender [:fake/rerender]))
              => :fake/rerender))
          (behavior "optional `:remote` key"
            (assertions
              (-> (f/commit-to-entity! :fake/component :remote true)
                second :remote)
              => true))))
      (component "commit-state - helper"
        (assertions
          (f/get-original-data basic-person :person/name)
          =fn=> #(not= % "MCQ")
          (-> basic-person
            (assoc :person/name "MCQ")
            (f/commit-state)
            (f/get-original-data :person/name))
          => "MCQ")))

    (component "reset-from-entity"
      (when-mocking
        (f/entity-x-form _ form-id xf) => (do (assertions
                                                form-id => :fake/form-id
                                                xf => f/reset-entity)
                                            ::ok)
        (let [reset-mut
              (m/mutate {:state (atom app-state)
                         :ast {:params {:remote true :form-id :fake/form-id}}}
                `f/reset-from-entity
                {:remote true
                 :form-id :fake/form-id})]
          (assertions
            (:remote reset-mut) => {:params {:form-id :fake/form-id}}
            ((:action reset-mut)) => ::ok)))
      (component "reset-from-entity! - api/public function"
        (when-mocking
          (om/transact! _ tx) => (fix-tx tx)
          (assertions
            (f/reset-from-entity! :fake/component basic-person)
            => `[f/reset-from-entity {:form-id [:people/by-id 3]}
                 f/validate-form {:form-id [:people/by-id 3]}
                 :ui/form-root])))
      (component "reset-entity"
        (assertions
          (-> basic-person
            (assoc :person/name "MCQ")
            (f/reset-entity)
            (f/current-value :person/name))
          =fn=> #(not= % "MCQ"))))))
