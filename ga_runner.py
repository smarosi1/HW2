import os
import sys
import shutil
import argparse
import subprocess
import re
import numpy as np
import pandas as pd
import matplotlib
import matplotlib.pyplot as plt
import scipy.stats as st

#Originally written by Richard Yarnell; I, Skylar Marosi, have modified it

# number of decimal places for rounding
DEC_PLACES   = 6

# main function
def run_main(args):

    if args.large_plots:
        font = {'weight' : 'bold',
                'size'   : 20}

        matplotlib.rc('font', **font)

    # read the experimental values from input file
    options, found_loop = get_options(args.params)

    # create output folder
    title    = options[0]['values'][0]
    out_path = 'results_' + title

    try:
        shutil.rmtree(out_path)
    except:
        pass

    os.mkdir(out_path)

    # loop over every experimental variable that has multiple test values.
    # if no such variable exists, 'loop' over the experiment name.
    for var_idx in range(len(options)):
        if not found_loop and var_idx == 0 or len(options[var_idx]['values']) > 1:
            run_loop(options, title, var_idx, out_path, args.optimal)

# loop over all desired values for a single experimental variable
def run_loop(options, title, var_idx, out_path, optimal):

    # get the name of the variable and the values
    var_name = options[var_idx]['var_name']
    values = options[var_idx]['values']

    # if possible, convert to numeric types and sort
    for i in range(len(values)):
        values[i] = int_or_float(values[i])

    values = sorted(values)

    # create a table to store the results for the different values
    summary = []
    summary_prefix = out_path + '/' + options[var_idx]['short_name']

    # loop over the values
    for val in values:

        # write a parameter file to be used by the java application
        file_prefix = out_path + '/' + options[var_idx]['short_name'] + '_' + str(val)
        params_file = file_prefix + '.params'

        print('Running %s test with %s = %s' % (title, var_name, val))

        results_file = write_params(options, val, var_idx, params_file)

        # execute java application
        subprocess.run(['java', 'Search', params_file], stdout = open(os.devnull, 'wb'))

        # get the results from all the runs and average them
        results  = get_results(results_file, optimal)
        averages = avg_results(results)

        # update summary with data from selected variable value
        summary_entry = get_summary(results, val)
        summary.append(summary_entry)

        # output tables and figures
        write_results(averages, file_prefix)
        plot_results(averages, file_prefix)
        plot_results_together(averages, file_prefix)

        # move the java application output file
        new_results_file = file_prefix + '_summary.txt'
        os.rename(results_file, new_results_file)

    # output summary of results for all tests over selected variable
    write_summary(summary, summary_prefix, options[var_idx])
    plot_summary (summary, summary_prefix, options[var_idx])

# read the experimental values from input file
def get_options(params_file):

    param_count = 0
    found_loop  = False
    options     = []

    # read parameter file
    file = open(params_file, 'r')
    lines = []

    for line in file:
        lines.append(line.strip())

    file.close()

    # loop over the lines of the file
    for line in lines:

        # stop reading file when found the notes
        if re.search('Notes:', line):
            break

        # found a parameter
        if re.search(':', line):

            # get variable name and the remaining text
            var_spaced, line = re.split(':', line, 1)

            # remove trailing white space and footnotes from variable names
            var = re.sub('\s*\Z', '', var_spaced)
            var = re.sub('\s*\(.*', '', var)

            # get the list of values to test
            values = re.split('\s+', line)

            # flag if found a variable to loop over
            if param_count > 0 and len(values) > 1:
                found_loop = True

            # save variable with and without white space and list of values
            options.append({'var_name_with_space': var_spaced, 'var_name': var, 'values': values})
            param_count += 1

    # make sure found the right number of variables
    if param_count != 15:
        sys.exit('Parameter file has wrong number of options')

    # parameters for each variable relating to use within this script
    options[ 0]['short_name'] = 'exp'  ; options[ 0]['style'] = 'bar'
    options[ 1]['short_name'] = 'prob' ; options[ 1]['style'] = 'bar'
    options[ 2]['short_name'] = 'file' ; options[ 2]['style'] = 'plot'
    options[ 3]['short_name'] = 'runs' ; options[ 3]['style'] = 'plot'
    options[ 4]['short_name'] = 'gens' ; options[ 4]['style'] = 'plot'
    options[ 5]['short_name'] = 'pop'  ; options[ 5]['style'] = 'plot'
    options[ 6]['short_name'] = 'sel'  ; options[ 6]['style'] = 'bar'
    options[ 7]['short_name'] = 'fit'  ; options[ 7]['style'] = 'bar'
    options[ 8]['short_name'] = 'xtype'; options[ 8]['style'] = 'bar'
    options[ 9]['short_name'] = 'xrate'; options[ 9]['style'] = 'plot'
    options[10]['short_name'] = 'mtype'; options[10]['style'] = 'plot'
    options[11]['short_name'] = 'mrate'; options[11]['style'] = 'plot'
    options[12]['short_name'] = 'seed' ; options[12]['style'] = 'plot'
    options[13]['short_name'] = 'genes'; options[13]['style'] = 'plot'
    options[14]['short_name'] = 'gsize'; options[14]['style'] = 'plot'

    options[ 6]['labels'] = ['', 'Proportional', 'Tournament', 'Random', 'Ranked']
    options[ 8]['labels'] = ['', 'Single Point', 'Two Point', 'Uniform']

    return options, found_loop

# write a parameter file
def write_params(options, var_val, var_idx, params_file):

    exp_name = ''

    # open a file to write
    file = open(params_file, 'w')

    # loop over all the variables
    for i in range(len(options)):

        # if writing the experimental variable, write the chosen value, otherwise write the default value
        if i == var_idx:
            val = var_val
        else:
            val = options[i]['values'][0]

        file.write(options[i]['var_name_with_space'] + ':' + str(val) + '\n')

        # capture the name of the experiment, which is in the first variable
        if i == 0:
            exp_name = val

    # close the file
    file.close()

    # return the name of what the results file will be
    results_file = exp_name + '_summary.txt'

    return results_file

# read a results file
def get_results(results_file, optimal):

    # initial declares
    results = {'best': [], 'avg': [], 'opt': []}
    found_size = False

    # read results file
    file = open(results_file, 'r')
    lines = []

    for line in file:
        lines.append(line.strip())

    file.close()

    # loop over the lines of the file backwards
    for line in reversed(lines):

        # look for run and generation numbers
        if re.search('\A\s*R\s+\d+\s+G\s+\d+', line):

            # extract data
            values = re.split('\s+', line)

            if len(values) != 7:
                print(values)
                sys.exit('Error in results file')

            _, run, _, gen, best, avg, _ = values

            run = int(run)
            gen = int(gen)

            # since reading the file backwards, can use data in last line to size the data array
            if not found_size:
                results['best'] = np.zeros([run, gen + 1])
                results['avg' ] = np.zeros([run, gen + 1])
                results['opt' ] = np.full(run, -1)
                found_size = True

            # save data under the appropriate run
            results['best'][run - 1][gen] = float(best)
            results['avg' ][run - 1][gen] = float(avg )

            # save generation number if found optimal value (working backwards, so always save)
            if optimal is not None:
                if float(best) == optimal:
                    results['opt'][run - 1] = gen

    return results

# calculate averages for an experiment
def avg_results(results):

    # calculate and return averages for each generation over all runs
    averages = {}

    averages['best'    ] = np.mean(results['best'], axis = 0)
    averages['best_std'] = np.std (results['best'], axis = 0)
    averages['avg'     ] = np.mean(results['avg' ], axis = 0)
    averages['avg_std' ] = np.std (results['avg' ], axis = 0)

    return averages

# write experiment results to a csv
def write_results(averages, file_prefix):

    # create a table with the generations moving across each row
    num_gen = averages['best'].shape[0]
    table = np.zeros([4, num_gen])

    table[0] = averages['best'    ]
    table[1] = averages['best_std']
    table[2] = averages['avg'     ]
    table[3] = averages['avg_std' ]

    # round everything and transpose so generations move down each column
    table = np.round(table, DEC_PLACES)
    table = table.transpose()

    # write the csv
    file =  file_prefix + '.csv'
    columns = ['Best', 'SD of Best', 'Average', 'SD of Average']
    df = pd.DataFrame(data = table, columns = columns)
    df.to_csv(file, index_label = 'Generation')

# plot experiment results
def plot_results(averages, file_prefix):

    # plot every variable separately
    for key, data in averages.items():

        file = file_prefix + '_' + key + '.jpg'

        plt.figure()

        if   key == 'best':
            plt.ylabel('Fitness')
            plt.title ('Best Fitness vs. Generation')

        elif key == 'best_std':
            plt.ylabel('Fitness')
            plt.title ('SD of Best Fitness vs. Generation')

        elif key == 'avg':
            plt.ylabel('Fitness')
            plt.title ('Average Fitness vs. Generation')

        elif key == 'avg_std':
            plt.ylabel('Fitness')
            plt.title ('SD of Average Fitness vs. Generation')

        plt.xlabel('Generation')

        # add one element to the Y values to match X (see below)
        blank = np.empty(1)
        blank[0] = np.nan
        data = np.append(data, blank)

        # add one element to the X values to stop at a round number
        x = range(0, len(data))
        xtick = max((round(len(data) / 5), 1))
        plt.xticks(np.arange(0, max(x) + 1, xtick))

        plt.plot(x, data)
        plt.savefig(file)
        plt.close()

# plot experiment results together
def plot_results_together(averages, file_prefix):

    file = file_prefix + '_all.jpg'

    plt.figure(figsize = (18, 12))  

    plt.xlabel('Generation')
    plt.ylabel('Fitness')
    plt.title ('Fitness vs. Generation')

    # add one element to the Y values to match
    blank = np.empty(1)
    blank[0] = np.nan

    first = False

    for key, data in averages.items():

        if   key == 'best':
            label = 'Best'
        elif key == 'best_std':
            label = 'SD of Best'
        elif key == 'avg':
            label = 'Average'
        elif key == 'avg_std':
            label = 'SD of Average'

        # add one element to the Y values to match X (see below)
        data = np.append(data, blank)

        # add one element to the X values to stop at a round number
        if not first:
            x = range(0, len(data))
            xtick = max((round(len(data) / 5), 1))
            plt.xticks(np.arange(0, max(x) + 1, xtick))
            first = True

        plt.plot(x, data, label = label)

    plt.legend()
    plt.savefig(file)
    plt.close()

# get overall results from a single experiment
def get_summary(results, var_val):

    all_best  = np.amax(results['best'], axis = 1)

    mean_best, std_best, conf_best = get_stats(all_best)

    optimal = [r for r in results['opt'] if r >= 0]
    mean_opt, std_opt, conf_opt = get_stats(optimal)

    return {'value'    : var_val,
            'mean_best': mean_best,
            'std_best' : std_best,
            'conf_best': conf_best,
            'mean_opt' : mean_opt,
            'std_opt'  : std_opt,
            'conf_opt' : conf_opt}

# calculate average, standard deviation, and confidence interval over all runs
def get_stats(data):

    if len(data) == 0:
        return '-', '-', '-'

    mean = np.round(np.mean(data), DEC_PLACES)
    std  = np.round(np.std (data), DEC_PLACES)

    if std > 0:
        conf   = st.t.interval(confidence = 0.95,
                               df = len(data) - 1,
                               loc = np.mean(data),
                               scale = st.sem(data))

    else:
        conf = [data[0], data[0]]

    conf = np.round(conf, DEC_PLACES)

    return mean, std, conf

# write summary results to a csv
def write_summary(summary, file_prefix, option):

    table = []

    file = file_prefix + '_summary.csv'

    # substitute labels for enumerated types
    for item in summary:
        new_item = item.copy()
        if option.get('labels'):
            if new_item['value'] < len(option['labels']):
                new_item['value'] = option['labels'][new_item['value']]
        table.append(list(new_item.values()))

    columns = [option['var_name'],
               'Mean Best of Run', 'SD of Best of Run', '95% CI of Best of Run',
               'Mean Earliest Optimal', 'SD of Earliest Optimal', '95% CI of Earliest Optimal']

    df = pd.DataFrame(data = table, columns = columns)

    df.to_csv(file, index = False)

# plot summary results
def plot_summary(summary, file_prefix, option):

    # build list of x and y values to plot
    x = []
    y = []

    for item in summary:
        x.append(item['value'])
        y.append(item['mean_best'])

    plt.figure()
    plt.ylabel('Fitness')
    plt.title ('Mean Best Fitness of Run vs. ' + option['var_name'])
    plt.xlabel(option['var_name'])

    if option['style'] == 'bar':

        # for a bar graph, attempt to look up x labels
        labels = []

        if option.get('labels'):

            # for each value, if a label exists, use it, otherwise use the original value
            for i in range(len(x)):
                if x[i] < len(option['labels']):
                    labels.append(option['labels'][x[i]])
                else:
                    labels.append(x[i])

        else:

            # if there are no labels, use the values as labels.
            # additionally, if the x values are strings, convert to integers so matlab can plot.
            for i in range(len(x)):
                labels.append(x[i])
                if isinstance(x[i], str):
                    x[i] = i

        plt.xticks(x, labels)
        plt.bar(x, y)

    else:

        # line graph
        plt.xticks(x)
        plt.plot(x, y)

    file = file_prefix + '_summary.jpg'
    plt.savefig(file)
    plt.close()

# convert strings from input file to numeric types
def int_or_float(str_in):

    try:
        return int(str_in)
    except:
        pass

    try:
        return float(str_in)
    except:
        pass

    return str_in

# read command line arguments and call main
if __name__ == '__main__':

    parser = argparse.ArgumentParser('Genetic algorithm runner')

    parser.add_argument('params',
                        type = str,
                        default = '',
                        help = 'Specify parameters file')

    parser.add_argument('-optimal',
                        type = float,
                        default = None,
                        help = 'Optimal fitness')

    parser.add_argument('-large_plots',
                        type = bool,
                        nargs = '?',
                        const = True,
                        default = False,
                        required = False,
                        help = 'Draw large plots')

    args = None
    args, unparsed = parser.parse_known_args()

    run_main(args)
