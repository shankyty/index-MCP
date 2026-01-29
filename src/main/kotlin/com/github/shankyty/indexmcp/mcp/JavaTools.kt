package com.github.shankyty.indexmcp.mcp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AllClassesSearch

object JavaTools {

    fun searchClasses(query: String, project: Project): List<String> {
        return ApplicationManager.getApplication().runReadAction<List<String>> {
            val scope = GlobalSearchScope.projectScope(project)
            // Ideally we would use a more fuzzy search like PsiShortNamesCache, but AllClassesSearch is safer for now.
            // But AllClassesSearch iterates ALL classes. That's heavy.
            // Let's use JavaPsiFacade to find class by name if query is qualified, or just filter.
            // Actually, for "search", users expect partial match.
            // PsiShortNamesCache.getInstance(project).getAllClassNames() returns an array of strings.
            // Let's use that for simple listing.

            // For this implementation, let's just use JavaPsiFacade to find a specific class,
            // or if we really want search, we iterate.
            // Since "search_classes" usually implies finding by name pattern.

            val result = mutableListOf<String>()

            // Only efficient way to search by name pattern in PSI is PsiShortNamesCache or FilenameIndex
            // Let's try to match by short name.
             com.intellij.psi.search.PsiShortNamesCache.getInstance(project).allClassNames.forEach { name ->
                if (name.contains(query, ignoreCase = true)) {
                    val classes = com.intellij.psi.search.PsiShortNamesCache.getInstance(project).getClassesByName(name, scope)
                    classes.forEach { psiClass ->
                        psiClass.qualifiedName?.let { result.add(it) }
                    }
                }
            }
            result.take(50) // Limit results
        }
    }

    fun getClassSource(className: String, project: Project): String? {
        return ApplicationManager.getApplication().runReadAction<String?> {
            val scope = GlobalSearchScope.projectScope(project)
            val psiClass = JavaPsiFacade.getInstance(project).findClass(className, scope)
            psiClass?.containingFile?.text
        }
    }

    fun listMethods(className: String, project: Project): List<String> {
        return ApplicationManager.getApplication().runReadAction<List<String>> {
            val scope = GlobalSearchScope.projectScope(project)
            val psiClass = JavaPsiFacade.getInstance(project).findClass(className, scope)
            psiClass?.methods?.map { method ->
                val params = method.parameterList.parameters.joinToString(", ") { "${it.type.presentableText} ${it.name}" }
                val returnType = method.returnType?.presentableText ?: "void"
                "$returnType ${method.name}($params)"
            } ?: emptyList()
        }
    }
}
