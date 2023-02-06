package dispenser

import (
	"context"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_DispenserClient_CommonTests(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		assert.Equal(t, "OAuth token", req.Header.Get("Authorization"))
		assert.Equal(t, "application/json", req.Header.Get("Accept"))
	}))
	defer server.Close()
	client := NewDispenserClient(server.URL, server.Client(), "token")
	_, _ = client.GetProjects(context.TODO())
}

func Test_DispenserClient_GetProjects(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		assert.Equal(t, "/db/api/v1/projects", req.URL.String())
		_, _ = rw.Write([]byte(`{"result":[{"key":"market","name":"Market","description":"","abcServiceId":186,"responsibles":{"persons":[],"yandexGroups":{}},"members":{"persons":[],"yandexGroups":{}},"parentProjectKey":"meta_market","subprojectKeys":["b2b","beruapps","checkouter","erpaxapta","global","market_report","marketassessor","marketcms","marketcontentdevops","marketfrontend","marketir","marketpers","marketsup","mbi","mbo","mmi","mstat","mstatcubevertica","sovetnik","vendors"],"person":null}]}`))
	}))
	defer server.Close()
	client := NewDispenserClient(server.URL, server.Client(), "token")
	projects, e := client.GetProjects(context.TODO())
	assert.NoError(t, e)
	assert.NotEmpty(t, projects)
	assert.Equal(t, 186, projects[0].AbcServiceID)
	assert.Equal(t, "market", projects[0].Key)
	assert.Equal(t, "Market", projects[0].Name)
	assert.Equal(t, "meta_market", projects[0].ParentProjectKey)
	assert.NotEmpty(t, projects[0].SubprojectKeys)
}

func Test_DispenserClient_GetProject(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		assert.Equal(t, "/db/api/v1/projects/market", req.URL.String())
		_, _ = rw.Write([]byte(`{"key":"market","name":"Market","description":"","abcServiceId":186,"responsibles":{"persons":[],"yandexGroups":{}},"members":{"persons":["robot-market-st","evlyubimov","apolenok","knifecult","alantukh","mironenkovb","nikitazaytsev","ifilippov5","zeithaste","katretyakova","dabanin","vdorogin","kgorelov","green-yeti","mdmship","vv-vasyukova","hvost239","ddtimofeeva","alexeybaranov","a-chechetkin","tishins","pavel-repin","glunchadze","m-krasavin","aostrikov","eugene-stor","strkate","alisa-qa","moonw1nd","fenruga","dphilippov87","shiko-mst","kolun","egorkozyrev","markin-nn","artemafk","marsel-a","shchukin","madiyetov","toliklunev","gallyamb","idonin","andreyberezin","mturina","vawsome","dragonmom","mslyusarenko","didimoner","extg","natalia-koz","m-mikhail","wanderer25","tzota","mafark","a-koptsov","kl1san","eurnyshev","plaks","audreen","camaro","phil-alekhin","nadya73","cevek","eyeless","a-kikin","imelnikov","sumltship","kanaglic","timoshenkoe","imdkravchenko","robot-tms-test","katarinish","robot-mrk-almk-test","bejibx","yumal","bugz-bunny","kvmultiship","bayshev","robot-tms-dev","sambuka","aahms","vatc","dboriskin","akostin","manushkin","yntv","dashared","imalyavin","myshov","aezhko","ermakov-n","egorkakshin","borkk","asimakov","ymoskalenk0","vismagilov","apopsuenko","alexahdp","tolya-baranov","okras","andjash","linkovdv","acamelot","irina-bodnar","a-bocharov","tesseract","zizu","vivg","titart","mtvv","akarchevski","altuninf","yzubchenko","robot-mrk-almk-dev","asvasilenko","b-vladi","kukabara","ashelshsky","l-baranova","jet-kuzmin","richard","apershukov","andreevdm","khabiroff","i-milyaev","hunmar","vvod","b-bari","smirnovpp","entarrion","m-simonenko79","yastremskiyd","afmn","volynskii","demian","aamironychev","dvalyaev","s10ly","vedmax","slyder","andrey-kustov","kachesov-a","dmitriydogaev","sbakanova","allerty","ippirina","sogreshilin","taffy","robot-mrk-almk-prod","wavefront","shomazz","savstavr","ares-ortega","ilya-ilin","andr-savel","kirmalishev","mrkutin","asafev","byrik","cornholio","i-marina","golovin-stan","dzvyagin","ibragimow","ushka","yaroshevsk","kudyas","sergeybutorin","caramelo","achugunov","jarith","lognick","pasdera","uid11","ivanov-af","maryz","trofim","anton0xf","naygeborin","antonymous","valeriyakilch","dsamuylov","semelianenko","rkikot","pixel","symultiship","eugenebakisov","elenala","bogdanov-se","kuith","e-abbakumov","ilya-khudy","alisa0","tooch42","omakovski","ayvorobyeva","anton19979","kniazev","alburhan","ychebotaev","oleynik","vsharando","crewman","ashevenkov","antonk52","vajs","maxkhol","lyubchich","ninok","yvgrishin","dcversus","volchnik","asolskaya","robot-mrk-ir-sandbox","predel","alumyan96","mshashi","rinat-s","timestep","robot-tms-prod","koshka","sergeykoles","olga-klu","ungomma","poluektov","dariaborisyak","elena-dolgova","denisk","le087","devrob1111","mvshmakovmv","isabirzyanov","ants","daria-petrova","koryakin-na","dukeartem","shumanskaya","gheljenor","muroming","alexey-semin","maria-nehved","riotta","a-burkan","jkt","aeronka","ttaaa","bzz13","ereznikova","pilyugin","aturkevich","zaharov-i","kornilovyv","topchyalexey","gbarkan","hauu","juice","a-anikina","antonkashkin","fess7","e-golov","karpenkoms","none23","artem-ios","v-dv","fdtd","vladon","orlov-denis","dtsyg","razmser","kvloginov","sikoroff","rodionidze","molodtsov","zhnick","shpizel","astvatsaturov","enload","iurik","yusokolova","feoktistov","lolipuli","inmltsp","lisynok","piupiu","gromoi","alex-aleshin","yuraaka","nafania","ushakova13","mkrasnoperov","dmpolyakov","senz","leonidlebedev","timshiryaev","ann1511","butov","mnatsakanyan","maxk","rodin-vv","alkedr","antilles","kristech","rkendzhaev","avetokhin","ts-slava","moisandrew","loudless","tamarintsev","borisova","avi2d","anclav","gaklimov","lesanra","lisenque","aavdonkin","afarshatov","zhdanova","ivalekseev","postsilver","vladbro","weed","abiryukov","marigovori","s-lebedev","sikora","varvara","sharonov","mkasumov","kurokikaze","mczim","fantamp","aisaev188","shift-red","alexkhait","tumryaeva","defruity","itasina","azdorov","annaerika","manvelova","ekbstudent","thesonsa","hjujgfg","belirafon"],"yandexGroups":{}},"parentProjectKey":"meta_market","subprojectKeys":["b2b","beruapps","checkouter","erpaxapta","global","market_report","marketassessor","marketcms","marketcontentdevops","marketfrontend","marketir","marketpers","marketsup","mbi","mbo","mmi","mstat","mstatcubevertica","sovetnik","vendors"],"person":null}`))
	}))
	defer server.Close()
	client := NewDispenserClient(server.URL, server.Client(), "token")
	project, e := client.GetProject(context.TODO(), "market")
	assert.NoError(t, e)
	assert.NotNil(t, project)
	assert.Equal(t, 186, project.AbcServiceID)
	assert.Equal(t, "market", project.Key)
	assert.Equal(t, "Market", project.Name)
	assert.Equal(t, "meta_market", project.ParentProjectKey)
	assert.NotEmpty(t, project.SubprojectKeys)
}

func Test_DispenserClient_GetQuotas(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		assert.Equal(t, "/db/api/v2/quotas", req.URL.String())
		_, _ = rw.Write([]byte(`{"result":[{"key":{"serviceKey":"mdb","resourceKey":"hdd","quotaSpecKey":"hdd-quota","projectKey":"market","segmentKeys":[]},"max":241892558110720,"actual":181419418583040,"lastOverquotingTs":null,"ownMax":0,"ownActual":0},{"key":{"serviceKey":"mdb","resourceKey":"ssd","quotaSpecKey":"ssd-quota","projectKey":"market","segmentKeys":[]},"max":9749575761920,"actual":9667841841152,"lastOverquotingTs":null,"ownMax":0,"ownActual":0},{"key":{"serviceKey":"mdb","resourceKey":"ram","quotaSpecKey":"ram-quota","projectKey":"market","segmentKeys":[]},"max":2055141851136,"actual":1688995889152,"lastOverquotingTs":null,"ownMax":0,"ownActual":0},{"key":{"serviceKey":"mdb","resourceKey":"cpu","quotaSpecKey":"cpu-quota","projectKey":"market","segmentKeys":[]},"max":462000,"actual":392500,"lastOverquotingTs":null,"ownMax":0,"ownActual":0}]}`))
	}))
	defer server.Close()
	client := NewDispenserClient(server.URL, server.Client(), "token")
	quotas, e := client.GetQuotas(context.TODO(), []string{})
	assert.NoError(t, e)
	assert.NotEmpty(t, quotas)
	assert.Len(t, quotas, 4)
}

func Test_DispenserClient_GetQuotasWithArgs(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		assert.Equal(t, "/db/api/v2/quotas?project=market", req.URL.String())
		_, _ = rw.Write([]byte(`{"result":[{"key":{"serviceKey":"mdb","resourceKey":"hdd","quotaSpecKey":"hdd-quota","projectKey":"market","segmentKeys":[]},"max":241892558110720,"actual":181419418583040,"lastOverquotingTs":null,"ownMax":0,"ownActual":0},{"key":{"serviceKey":"mdb","resourceKey":"ssd","quotaSpecKey":"ssd-quota","projectKey":"market","segmentKeys":[]},"max":9749575761920,"actual":9667841841152,"lastOverquotingTs":null,"ownMax":0,"ownActual":0},{"key":{"serviceKey":"mdb","resourceKey":"ram","quotaSpecKey":"ram-quota","projectKey":"market","segmentKeys":[]},"max":2055141851136,"actual":1688995889152,"lastOverquotingTs":null,"ownMax":0,"ownActual":0},{"key":{"serviceKey":"mdb","resourceKey":"cpu","quotaSpecKey":"cpu-quota","projectKey":"market","segmentKeys":[]},"max":462000,"actual":392500,"lastOverquotingTs":null,"ownMax":0,"ownActual":0}]}`))
	}))
	defer server.Close()
	client := NewDispenserClient(server.URL, server.Client(), "token")
	quotas, e := client.GetQuotas(context.TODO(), []string{"market"})
	assert.NoError(t, e)
	assert.NotEmpty(t, quotas)
	assert.Len(t, quotas, 4)
}

func Test_DispenserClient_SetMaxQuota(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		assert.Equal(t, "/db/api/v1/services/mdb/sync-state/quotas/set", req.URL.String())
		bytes, e := ioutil.ReadAll(req.Body)
		assert.NoError(t, e)
		assert.NotEmpty(t, bytes)
		_, _ = rw.Write([]byte(`{"result": []}`))
	}))
	client := NewDispenserClient(server.URL, server.Client(), "token")
	res, err := client.SetMaxQuota(context.TODO(), []QuotaMaxChangeRequest{{
		ProjectKey:  "MARKETITO",
		ResourceKey: "cpu",
		Max: quotaValue{
			Value: 9500,
			Unit:  "PERMILLE_CORES",
		},
	}})
	assert.NoError(t, err)
	assert.NotNil(t, res)
}
