// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.compose

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import java.util.Locale
import org.jetbrains.kotlin.psi.KtFunction
import slack.lint.compose.util.isTypeUnstableCollection
import slack.lint.util.Priorities
import slack.lint.util.sourceImplementation

class ComposeUnstableCollectionsDetector : ComposableFunctionDetector(), SourceCodeScanner {

  companion object {
    private val DiamondRegex by lazy(LazyThreadSafetyMode.NONE) { Regex("<.*>\\??") }
    private val String.capitalized: String
      get() = replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
      }

    fun createErrorMessage(type: String, rawType: String, variable: String) =
      """
        The Compose Compiler cannot infer the stability of a parameter if a $type is used in it, even if the item type is stable.\
        You should use Kotlinx Immutable Collections instead: `$variable: Immutable$type` or create an `@Immutable` wrapper for this class: `@Immutable data class ${variable.capitalized}$rawType(val items: $type)`\
        \
        See https://twitter.github.io/compose-rules/rules/#avoid-using-unstable-collections for more information.
      """
        .trimIndent()

    val ISSUE =
      Issue.create(
        id = "ComposeModifierWithoutDefault",
        briefDescription = "Immutable collections should be used in Composables",
        explanation = "This is replaced when reported",
        category = Category.PRODUCTIVITY,
        priority = Priorities.NORMAL,
        severity = Severity.ERROR,
        implementation = sourceImplementation<ComposeUnstableCollectionsDetector>()
      )
  }

  override fun visitComposable(context: JavaContext, function: KtFunction) {
    for (param in function.valueParameters.filter { it.isTypeUnstableCollection }) {
      val variableName = param.nameAsSafeName.asString()
      val type = param.typeReference?.text ?: "List/Set/Map"
      val message =
        createErrorMessage(
          type = type,
          rawType = type.replace(DiamondRegex, ""),
          variable = variableName
        )
      val targetToReport = param.typeReference ?: param
      context.report(
        ISSUE,
        targetToReport,
        context.getLocation(targetToReport),
        message,
      )
    }
  }
}
