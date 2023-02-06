package berulockers2yt

import (
	"sort"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func TestFindTabelsForDel(t *testing.T) {
	var createTables = func(dayStart, dayEnd, hourStart, hourEnd int, tstart time.Time) []string {
		tablesForPeriod := make([]string, 0)
		tstart = tstart.Add(-time.Duration(tstart.Hour())*time.Hour -
			time.Duration(tstart.Minute())*time.Minute -
			time.Duration(tstart.Second())*time.Second)
		for day := dayStart; day < dayEnd; day++ {
			for hour := hourStart; hour < hourEnd; hour++ {
				tablesForPeriod = append(tablesForPeriod,
					tstart.AddDate(0, 0, day).Add(time.Duration(hour)*time.Hour).Format("20060102_150405"))
			}
		}
		return tablesForPeriod
	}

	tstart, _ := time.Parse("2006-01-02 15:04:05", "2021-06-18 12:33:13")
	//Много дней на удаление
	strsTest1 := createTables(-10, 1, 0, 24, tstart)
	delsTest1 := FindTabelsForDel(&strsTest1, tstart)
	ansTest1 := createTables(-10, -7, 1, 24, tstart)
	sort.Strings(ansTest1)
	sort.Strings(delsTest1)
	require.Equal(t, delsTest1, ansTest1)

	//Все хорошо, удалять не надо
	strsTest2 := createTables(-20, -7, 0, 1, tstart)
	strsTest2 = append(strsTest2, createTables(-7, 1, 0, 24, tstart)...)
	delsTest2 := FindTabelsForDel(&strsTest2, tstart)
	require.Equal(t, delsTest2, make([]string, 0))

	//Удалить один день (нормальная работа)
	strsTest3 := createTables(-20, -8, 0, 1, tstart)
	strsTest3 = append(strsTest3, createTables(-8, 1, 0, 24, tstart)...)
	delsTest3 := FindTabelsForDel(&strsTest3, tstart)
	ansTest3 := createTables(-8, -7, 1, 24, tstart)
	sort.Strings(ansTest3)
	sort.Strings(delsTest3)
	require.Equal(t, delsTest3, ansTest3)

	//Пустой ввод
	strsTest4 := make([]string, 0)
	delsTest4 := FindTabelsForDel(&strsTest4, tstart)
	require.Equal(t, delsTest4, make([]string, 0))
}

/*
//----Просто тестик----
func TestFoo(t *testing.T) {
	config := MakeDevConfig("", "//tmp/kogotkovd/combinator/beru_lockers")
	var createTables = func(days, daye, hours, houre int) []string {
		ans := make([]string, 0)
		tstrat := time.Now()
		tstrat = tstrat.Add(-time.Duration(tstrat.Hour())*time.Hour -
			time.Duration(tstrat.Minute())*time.Minute -
			time.Duration(tstrat.Second())*time.Second)
		for day := days; day < daye; day++ {
			for hour := hours; hour < houre; hour++ {
				ans = append(ans, tstrat.AddDate(0, 0, day).Add(time.Duration(hour)*time.Hour).Format("20060102_150405"))
			}
		}
		return ans
	}

	client, err := ythttp.NewClient(&yt.Config{Proxy: config.cluster, Token: "", ReadTokenFromFile: true})
	require.NoError(t, err)
	tx, err := client.BeginTx(context.Background(), nil)
	require.NoError(t, err)
	defer func() { _ = tx.Abort()}()

	toWrite := make([]BeruLockersRow, 10)
	for _, table := range createTables(-10, 0, 0, 3) {
		err = WriteToYT(config, "/"+table, toWrite, tx)
		require.NoError(t, err)
		fmt.Println("writed: ", table)
	}
	tx.Commit()
	//require.NoError(t, UploadOnce(config))
}
*/
