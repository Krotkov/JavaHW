#!/bin/bash
tasks=( "walk RecursiveWalk RecursiveWalk"
	"arrayset NavigableSet ArraySet"
	"student AdvancedStudentGroupQuery StudentDB"
	"implementor class Implementor"
	"implementor jar-class Implementor"
	"implementor jar-class Implementor"
	"concurrent list IterativeParallelism"
	"mapper list ParallelMapperImpl,ru.ifmo.rain.krotkov.concurrent.IterativeParallelism"
	"crawler hard WebCrawler"
	""
	""
)
case $1 in
    10c)
        bash run.sh hello client-i18n HelloUDPClient $2
        ;;
    10s)
        bash run.sh hello server-i18n HelloUDPServer $2
        ;;
    *)
	    ./run.sh ${tasks[(($1-1))]} $2
        ;;
esac
