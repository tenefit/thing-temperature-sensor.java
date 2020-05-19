## Creating a new Java repository in Reaktivity

1. Name the new repository `[repository-name].java` with `README` only
2. Enable issues only (no wiki or restricted wiki edits)
3. Create a new branch called `develop`
4. Make the `develop` branch the default branch
5. Protect the `develop` branch (check everything except "Include Administrators")
6. Protect the `master` branch (check only "Protect this branch")
7. Clone the new repository locally, then
```bash
$ git config merge.ours.driver true
$ git remote add --track develop build https://github.com/tenefit/build-template.java
$ git fetch build develop
$ git merge build/develop --allow-unrelated-histories --no-commit
```
Review the changes, modify the `pom.xml` to reflect your project `name`, `description`, and `artifactId`.
Commit the changes and push them back.
```bash
$ git add pom.xml
$ git commit
$ git push origin develop
```
Travis CI requires environment entries for `TENEFIT_USERNAME` and `TENEFIT_PASSWORD` for use in `.travis/maven-settings.xml`.
```bash
$ travis encrypt "TENEFIT_USERNAME=[build-user]" --add env.global
$ travis encrypt "TENEFIT_PASSWORD=[access-token]" --add env.global
$ git add .travis.yml
$ git commit -m "Access Tenefit Maven repository"
$ git push origin develop
```
Verify the Travis CI build can resolve dependencies via Tenefit Maven repository.
