package dev.jack.blpatches.build;

import java.nio.file.Path;
import java.util.List;

import org.gradle.api.Project;

public final class BetweenlandsPatchSettings {

    public static final String DEFAULT_UPSTREAM_URL = "https://github.com/Angry-Pixel/The-Betweenlands.git";
    public static final String DEFAULT_UPSTREAM_BRANCH = "1.7.10";
    public static final String DEFAULT_OFFICIAL_JAR_URL =
            "https://maven.fentanylsolutions.org/releases/thebetweenlands/TheBetweenlands/1.0.6-alpha/TheBetweenlands-1.0.6-alpha-universal.jar";
    public static final String DEFAULT_OFFICIAL_JAR_NAME = "TheBetweenlands-1.0.6-alpha-universal.jar";
    public static final String DEFAULT_OFFICIAL_JAR_SHA256 =
            "beb21f21a40dc0014096124226d4ae90cc1286461a359947ff07211dff1be3e0";

    private static final List<String> JAVA_WORKSPACE_ROOTS = List.of("java");
    private static final List<String> RESOURCE_WORKSPACE_ROOTS = List.of("resources");
    private static final List<String> WORKSPACE_ROOTS = List.of("java", "resources");

    private final Path projectDir;
    private final String upstreamUrl;
    private final String upstreamBranch;
    private final String officialJarUrl;
    private final String officialJarName;
    private final String officialJarSha256;

    private BetweenlandsPatchSettings(
            Path projectDir,
            String upstreamUrl,
            String upstreamBranch,
            String officialJarUrl,
            String officialJarName,
            String officialJarSha256) {
        this.projectDir = projectDir;
        this.upstreamUrl = upstreamUrl;
        this.upstreamBranch = upstreamBranch;
        this.officialJarUrl = officialJarUrl;
        this.officialJarName = officialJarName;
        this.officialJarSha256 = officialJarSha256;
    }

    public static BetweenlandsPatchSettings from(Project project) {
        Path projectDir = project.getProjectDir().toPath();
        String upstreamUrl = stringProperty(project, "betweenlandsUpstreamUrl", DEFAULT_UPSTREAM_URL);
        String upstreamBranch = stringProperty(project, "betweenlandsUpstreamBranch", DEFAULT_UPSTREAM_BRANCH);
        String officialJarUrl = stringProperty(project, "betweenlandsOfficialJarUrl", DEFAULT_OFFICIAL_JAR_URL);
        String officialJarName = stringProperty(project, "betweenlandsOfficialJarName", DEFAULT_OFFICIAL_JAR_NAME);
        String officialJarSha256 = stringProperty(project, "betweenlandsOfficialJarSha256", DEFAULT_OFFICIAL_JAR_SHA256);
        return new BetweenlandsPatchSettings(
                projectDir,
                upstreamUrl,
                upstreamBranch,
                officialJarUrl,
                officialJarName,
                officialJarSha256);
    }

    private static String stringProperty(Project project, String name, String defaultValue) {
        Object value = project.findProperty(name);
        return value == null ? defaultValue : value.toString().trim();
    }

    public Path getProjectDir() {
        return projectDir;
    }

    public String getUpstreamUrl() {
        return upstreamUrl;
    }

    public String getUpstreamBranch() {
        return upstreamBranch;
    }

    public String getOfficialJarUrl() {
        return officialJarUrl;
    }

    public String getOfficialJarName() {
        return officialJarName;
    }

    public String getOfficialJarSha256() {
        return officialJarSha256;
    }

    public String getOfficialJarPrefix() {
        int dash = officialJarName.indexOf('-');
        int dot = officialJarName.indexOf('.');
        int end = dash >= 0 ? dash : dot >= 0 ? dot : officialJarName.length();
        return officialJarName.substring(0, end);
    }

    public List<String> getWorkspaceRoots() {
        return WORKSPACE_ROOTS;
    }

    public List<String> getJavaWorkspaceRoots() {
        return JAVA_WORKSPACE_ROOTS;
    }

    public List<String> getResourceWorkspaceRoots() {
        return RESOURCE_WORKSPACE_ROOTS;
    }

    public Path getMetadataRoot() {
        return projectDir.resolve(".betweenlands");
    }

    public Path getUpstreamDir() {
        return getMetadataRoot().resolve("upstream");
    }

    public Path getCacheDir() {
        return getMetadataRoot().resolve("cache");
    }

    public Path getOfficialJarPath() {
        return getCacheDir().resolve(officialJarName);
    }

    public Path getWorkspaceDir() {
        return projectDir.resolve("betweenlands-src");
    }

    public Path getPatchDir() {
        return projectDir.resolve("patches");
    }

    public Path getWorkspacePatchFile() {
        return getPatchDir().resolve("workspace.patch");
    }
}
