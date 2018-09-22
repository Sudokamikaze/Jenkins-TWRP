import groovy.json.JsonSlurper

try {
    List<String> artifacts = new ArrayList<String>()
    def artifactsUrl = "https://raw.githubusercontent.com/Sudokamikaze/TWRP-tools/master/device-targets.json"          
    def artifactsObjectRaw = ["curl", "-s", "-H", "accept: application/json", "-k", "--url", "${artifactsUrl}"].execute().text
    def jsonSlurper = new JsonSlurper()
    def artifactsJsonObject = jsonSlurper.parseText(artifactsObjectRaw)
    def dataArray = artifactsJsonObject.data
    for(item in dataArray){
        artifacts.add(item.text)
    } 
    return artifacts
} catch (Exception e) {
    print "There was a problem fetching the artifacts"
}