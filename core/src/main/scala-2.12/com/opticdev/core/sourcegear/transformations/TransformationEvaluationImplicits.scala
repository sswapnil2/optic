package com.opticdev.core.sourcegear.transformations

import com.opticdev.sdk.descriptions.Transformation
import play.api.libs.json.JsObject

object TransformationEvaluationImplicits {
  implicit class TransformationWithEvaluation(transformation: Transformation) {

    def applyTo(jsObject: JsObject) = {



    }

  }
}