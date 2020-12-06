library("ggplot2")
theme_set(theme_bw())
library("sf")
library("rnaturalearth")
library("rnaturalearthdata")

world <- ne_countries(scale = "medium", returnclass = "sf")
class(world)
locations = read.table("IPLocations.txt", header = F, sep = ',')
locationsData = na.omit(data.frame(as.double(as.character(locations$V2)), as.double(as.character(locations$V3))))
sites = data.frame(longitude = locationsData[,2], latitude = locationsData[,1])
(sites <- st_as_sf(sites, coords = c("longitude", "latitude"), crs = 4326, agr = "constant"))
ggplot(data = world) +
    geom_sf() +
    geom_sf(data = sites, size = 4, shape = 23, fill = "darkred")  
#    coord_sf(xlim = c(-90, 90), ylim = c(-90, 90), expand = FALSE)
ggsave("worldmap.png")
