------------sample_cases folder---------------

In each folder
1. Sample input: one *****.txt file with randomly-generated node list (three columns: x, y, initial communication range)

2. one result.txt file with counts of packets for three configurations: two fuzzy logic and one non-fuzzy logic

3. three subfolder: FL1 and FL2 are for fuzzy logic configuration 1 and 2, NoFL is for non-fuzzy logic

4. each subfolder includes: 
	a. connected_graph folder: original graph, graphs after update in each round
	b. shortest_path_tree folder: calculated shortest path trees for graphs after update in each round
	c. energy_chart_****.png: total remaining energy of WSN after each round





------------source_code folder----------------


1. Main class is in TermProject.java. Also included are supporting classes. To compile, a jfreechart library need to be downloaded from http://sourceforge.net/projects/jfreechart/files/, and then from the unzipped folder, import the jcommon-1.0.23.jar and jfreechart-1.0.19.jar in lib folder.

2. When running the program, first choose whether to input from file or to do use data generation. Next, choose whether to use fuzzy logic or not. If choose to use fuzzy logic, configuration 1 or 2 need to be selected.

3. The resulting files include png files of graph and corresponding shortest paths to base station from available sensor nodes (labeled by cycle number and timestamp, for example, 1-45767.png and 1-65657.png is the network graph and shortest path tree for cycle 1).

4. Results also include a png picture (eg. energy_chart_16214.png) showing the total remaining energy of the system VS cycle number.