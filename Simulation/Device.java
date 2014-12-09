public class Device
{
	protected static final int STATE_BC = 0; 
	protected static final int STATE_INT = 1;
	protected static final int STATE_PRE = 2;

	//preloaded
	protected int _startingState;
	protected int _startingTimePercent; //in percentage
	protected int _preInRangeTime;
	protected int _stayInTime;

	//calcualted during run time
	protected int _startingTime; // in seconds

	//during run time
	protected int _bcDuration;
	protected int _bcIntermission;
	protected int _currentTime;
	protected int _currentState;
	protected int _currentStayInTime;

	public static Device create(int preInRangeTimeMax) //seconds
	{
		Device d = new Device();

		//random a state to start from
		d._startingState = (int) (Math.random() * 100) % 2;

		//random a current broadcasting/intermission time (in %, since don't know the bcD and bcI)
		d._startingTimePercent = (int) (Math.random() * 1000) % 100 + 1; // 1 to 100
		d._stayInTime = (int) (Math.random() * 100) % 60 + 1; //minimum 1 second, max 60 seconds

		//amount of seconds to wait before entering range of target
		d._preInRangeTime = (int)(Math.random() * 1000000) % preInRangeTimeMax; //within 15 mins

		return d;
	}

	public static Device load(String dataString)
	{
		String[] data = dataString.split(",");

		Device d = new Device();
		d._startingState = Integer.parseInt(data[0]);
		d._startingTimePercent = Integer.parseInt(data[1]);
		d._preInRangeTime = Integer.parseInt(data[2]);
		d._stayInTime = Integer.parseInt(data[3]);

		return d;
	}

	public void init(int bcDuration, int bcIntermission) //for fixed intermission setup
	{
		_bcDuration = bcDuration;
		_bcIntermission = bcIntermission;

		//start with pre in range settings
		_currentTime = _preInRangeTime;
		_currentState = STATE_PRE;
		_currentStayInTime = _stayInTime;

		//calculate proportional starting time according to the state
		if (_startingState == STATE_BC)
			_startingTime = (int)(_bcDuration / 100.0 * _startingTimePercent);
		else if (_startingState == STATE_INT)
		{
			_startingTime = (int)(_bcIntermission / 100.0 * _startingTimePercent);
			_startingTime %= _stayInTime; // to ensure that there is at least 1 second of broadcast time during the stay in period
		}
	}

	public void timePassed()
	{
		_currentTime--;
		//reduce the stay in time only when Device is already within range
		if (_currentState != STATE_PRE)
			_currentStayInTime--;

		if (_currentTime > 0)
			return;

		//state change
		switch (_currentState)
		{
		case STATE_PRE:
			_currentState = _startingState; //preloaded starting state
			_currentTime = _startingTime; //preloaded starting time
			break;
		case STATE_BC:
			_currentState = STATE_INT; //go to intermission after broadcast
			_currentTime = _bcIntermission;
			break;
		case STATE_INT:
			_currentState = STATE_BC; //go to broadcast after intermission
			_currentTime = _bcDuration;
			break;
		}
	}

	public boolean canMeet()
	{
		return _currentState == STATE_BC;
	}

	public boolean hasExited()
	{
		return _currentStayInTime <= 0;
	}

	public String toDataString()
	{
		return _startingState + "," + _startingTimePercent + "," + _preInRangeTime + "," + _stayInTime + "\n";
	}
}
