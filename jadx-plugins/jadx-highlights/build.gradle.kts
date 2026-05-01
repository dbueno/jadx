plugins {
	id("jadx-library")
}

dependencies {
	api(project(":jadx-core"))

	implementation("com.google.code.gson:gson:2.13.2")
}
