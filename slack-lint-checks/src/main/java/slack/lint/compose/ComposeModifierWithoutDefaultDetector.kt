// Copyright (C) 2023 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.compose

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtFunction
import slack.lint.compose.util.definedInInterface
import slack.lint.compose.util.isAbstract
import slack.lint.compose.util.isActual
import slack.lint.compose.util.isModifier
import slack.lint.compose.util.isOverride
import slack.lint.compose.util.lastChildLeafOrSelf
import slack.lint.util.Priorities
import slack.lint.util.sourceImplementation

class ComposeModifierWithoutDefaultDetector : ComposableFunctionDetector(), SourceCodeScanner {

  companion object {
    val ISSUE =
      Issue.create(
        id = "ComposeModifierWithoutDefault",
        briefDescription = "Missing Modifier default value",
        explanation =
          """
              This @Composable function has a modifier parameter but it doesn't have a default value.\
              \
              See https://twitter.github.io/compose-rules/rules/#modifiers-should-have-default-parameters for more information.
            """
            .trimIndent(),
        category = Category.PRODUCTIVITY,
        priority = Priorities.NORMAL,
        severity = Severity.ERROR,
        implementation = sourceImplementation<ComposeModifierWithoutDefaultDetector>()
      )
  }

  override fun visitComposable(context: JavaContext, function: KtFunction) {
    if (
      function.definedInInterface || function.isActual || function.isOverride || function.isAbstract
    )
      return

    // Look for modifier params in the composable signature, and if any without a default value is
    // found, error out.
    function.valueParameters
      .filter { it.isModifier }
      .filterNot { it.hasDefaultValue() }
      .forEach { modifierParameter ->

        // This error is easily auto fixable, we just inject ` = Modifier` to the param
        val lastToken = modifierParameter.node.lastChildLeafOrSelf() as LeafPsiElement
        val currentText = lastToken.text
        context.report(
          ISSUE,
          modifierParameter,
          context.getLocation(modifierParameter),
          ISSUE.getExplanation(TextFormat.TEXT),
          fix()
            .replace()
            .name("Add '= Modifier' default value.")
            .range(context.getLocation(modifierParameter))
            .shortenNames()
            .text(currentText)
            .with("$currentText = Modifier")
            .autoFix()
            .build()
        )
      }
  }
}
