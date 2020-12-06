#install.packages("TSP")
#install.packages("tspmeta")
library(TSP)
library(tspmeta)
numberOfRepairCrews = 3
navy = read.table(file = "CaseStudy5.csv", header = T, sep =",")
loc = data.frame(navy$Node.X, navy$Node.Y)
#loc = data.frame(navy$Node.X, navy$Node.Y, navy$Node.Importance)
clustering = kmeans(na.omit(loc), centers = numberOfRepairCrews, iter.max = 30)
png("routes.png")
plot(na.omit(loc), col = clustering$cluster)

for(i in 1:numberOfRepairCrews)
{
    group = na.omit(loc)[which(clustering$cluster == i),]
    print(group)

    groupX = group$navy.Node.X
    groupY = group$navy.Node.Y

    for(z in 1:length(groupX))
    {
        #groupX[z] = groupX[z] * group$navy.Node.Importance[z]
        #groupY[z] = groupY[z] * group$navy.Node.Importance[z]
        #print(groupX[z])
    }

    if(length(groupX) > 1)
    {
        groupFrame = data.frame(long = group$navy.Node.X, lat = group$navy.Node.Y)
        groupMatrix = as.matrix(groupFrame)
        distData = dist(groupMatrix)
        tsp.ins = tsp_instance(groupMatrix, distData)
        tour = run_solver(tsp.ins, method = "2-opt")
        print(tour)
    }



    nodeCount = length(groupX)
    print(nodeCount)


    if(nodeCount > 1)
    {
        for(x in 1:(nodeCount - 1))
        {
            print(x)
            sX = groupX[x]
            sY = groupY[x]
            cat(sX, " ", sY, "\n")
            eX = groupX[x+1]
            eY = groupY[x+1]
            arrows(sX, sY, eX, eY, col = i)
        }
    }
}
dev.off()
browseURL("routes.png")
