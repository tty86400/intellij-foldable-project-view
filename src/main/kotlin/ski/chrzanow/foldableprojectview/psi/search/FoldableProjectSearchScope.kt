package ski.chrzanow.foldableprojectview.psi.search

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessModuleDir
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectFileIndex
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import ski.chrzanow.foldableprojectview.settings.FoldableProjectSettings
import kotlin.io.path.pathString
import kotlin.io.path.relativeToOrNull

class FoldableProjectSearchScope(project: Project, val settings: FoldableProjectSettings, val rule: String) : GlobalSearchScope(project) {

    private val fileIndex = ProjectFileIndex.getInstance(project)
    private fun String.caseSensitive() = when (settings.caseSensitive) {
        true -> this
        false -> this.lowercase()
    }

    private val pattern = try {
        FileSystems.getDefault().getPathMatcher("glob:${rule.caseSensitive()}")
    } catch (e: Exception) {
        null
    }

    override fun contains(file: VirtualFile): Boolean {
        if (pattern == null) {
            return false
        }

        val moduleDir = fileIndex.getModuleForFile(file)?.guessModuleDir() ?: project?.guessProjectDir() ?: return false
        val base = moduleDir.toNioPath()
        val relativePath = file.toNioPath().relativeToOrNull(base) ?: return false
        
        // Convert to string with case sensitivity applied
        val pathString = relativePath.pathString.caseSensitive()
        
        // Create a path from the string for matching
        val pathToMatch = try {
            java.nio.file.Paths.get(pathString)
        } catch (e: Exception) {
            return false
        }

        return pattern.matches(pathToMatch)
    }

    override fun isSearchInModuleContent(aModule: Module) = true

    override fun isSearchInLibraries() = false

    override fun getDisplayName() = rule

    override fun toString() = rule
}
