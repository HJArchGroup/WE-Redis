#Usage of SysInfo Command#

A new category `sysinfo` is introduced into the `INFO` command, and the system information can be obtained through the following ways:

	INFO sysinfo | INFO all

The response of `INFO sysinfo` consists of the following information:

	up_time		: Seconds since boot
	load_1_min	: 1 minute load averages
	load_5_min	: 5 minute load averages
	load_15_min	: 15 minute load averages
	ram_total	: Total usable main memory size
	ram_free	: Available memory size
	ram_shared	: Amount of shared memory
	ram_buffer	: Memory used by buffers
	swap_total	: Total swap space size
	swap_free	: Swap space still available
	processes	: Number of current processes

The original response of `INFO all` would be appended the information above.
