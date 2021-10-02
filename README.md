# clj-tf

Helper to register (namespaced) keywords from Terraform provider schemas. Allows basic "intellisense", when writing Terraform as EDN.

```
$ terraform init

Initializing the backend...

Initializing provider plugins...
- Reusing previous version of hashicorp/aws from the dependency lock file
- Installing hashicorp/aws v3.60.0...
- Installed hashicorp/aws v3.60.0 (signed by HashiCorp)

Terraform has been successfully initialized!

You may now begin working with Terraform. Try running "terraform plan" to see
any changes that are required for your infrastructure. All Terraform commands
should now work.

If you ever set or change modules or backend configuration for Terraform,
rerun this command to reinitialize your working directory. If you forget, other
commands will detect it and remind you to do so if necessary.

$ clojure -M:repl/rebel
nREPL server started on port 52370 on host localhost - nrepl://localhost:52370
[Rebel readline] Type :repl/help for online help info
user=> (require '[clj-tf.core :as clj-tf])
nil
user=> (clj-tf/init-namespaced-keywords)
nil
user=> :aws_instance ;; <- Hit tab
:aws_instance         (k)   :aws_instance/filter  (k)   :aws_instance/tags    (k)   :aws_instances/ids    (k)
:aws_instance/ami     (k)   :aws_instance/host_id (k)   :aws_instances        (k)
:aws_instance/arn     (k)   :aws_instance/id      (k)   :aws_instances/id     (k)

;; Nested blocks/attributes are separate by "." in the keyword namespace
user=> :aws_instance.ca ;; <- Hit tab
user=> :aws_instance.capacity_reservation_specification
:aws_instance.capacity_reservation_specification.capacity_reservation_target/capacity_reservation_id (k)
:aws_instance.capacity_reservation_specification/capacity_reservation_preference                     (k)
:aws_instance.capacity_reservation_specification/capacity_reservation_target                         (k)

;; Then start writing resources
user=> (def iac
  #_=>   {:resource
  #_=>    {:aws_instance
  #_=>     {:worker
  #_=>      {:aws_instance/a  ;; Below are suggestions for namespaced keys that fit for :aws_instance
:aws_instance         (k)   :aws_instance/filter  (k)   :aws_instance/tags    (k)   :aws_instances/ids    (k)
:aws_instance/ami     (k)   :aws_instance/host_id (k)   :aws_instances        (k)
:aws_instance/arn     (k)   :aws_instance/id      (k)   :aws_instances/id     (k)

;; Make a complete resource data
user=> (def iac
  #_=>   {:resource
  #_=>    {:aws_instance
  #_=>     {:worker
  #_=>      {:aws_instance/ami "foo"
  #_=>       :aws_instance/instance_type "t4g.nano"}}}})
#'user/iac
user=> (clj-tf/write-json iac "main.tf.json")
nil

# Then run plan

$ terraform plan

Terraform used the selected providers to generate the following execution plan. Resource actions are indicated with the following symbols:
  + create

Terraform will perform the following actions:

  # aws_instance.worker will be created
  + resource "aws_instance" "worker" {
      + ami                                  = "foo"
      + arn                                  = (known after apply)
      + associate_public_ip_address          = (known after apply)
      ...
```
