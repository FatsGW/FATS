
public class Receiver
{
	public static final int INTERMISSION_MAX = 120;
	public static final int INTERMISSION_MIN = 10;
	private int INTERMISSION_STEP = 3;

	private int _bcDuration;
	private int _bcIntermission;

	//results
	private int _bcCount;
	private int _totalBcDuration;
	private int _hit;
	private int _miss;

	//timer and states
	private final int STATE_BC = 1;
	private final int STATE_INT = 2;

	private boolean _isExtending;
	private final boolean _isExtensionEnabled = false ;
	private final boolean _isPowerSavingEnabled = true;

	private int _timer;
	private int _currentState;
	private int _intermission_step;

	//averaging
	private int _sumCount;

	public Receiver(int bcDuration, int bcIntermission)
	{
		_bcDuration = bcDuration;
		_bcIntermission = bcIntermission;

		_timer = _bcDuration;
		_currentState = STATE_BC;
		_isExtending = false;
		_intermission_step = 0;

		//set all results to 0
		_totalBcDuration = _hit = _miss = 0;
		_bcCount = 1;
	}

	public Receiver()
	{
		_bcCount = _totalBcDuration = _hit = _miss = 0;
		_sumCount = 0;
	}

	public void sum(Receiver r)
	{
		_bcDuration = r._bcDuration;
		_bcIntermission = r._bcIntermission;


		_bcCount += r._bcCount;
		_totalBcDuration += r._totalBcDuration;
		_hit += r._hit;
		_miss += r._miss;
		_sumCount++;
	}

	public void average()
	{
		_bcCount /= _sumCount;
		_totalBcDuration /= _sumCount;
		_hit /= _sumCount;
		_miss /= _sumCount;
	}

	public void timePassed()
	{
		_timer--;

		if (_timer >= 0)
		{
			if (_currentState == STATE_BC)
				_totalBcDuration++;
			return;
		}

		switch(_currentState)
		{
			case STATE_BC:
				if (_isExtensionEnabled && _isExtending)
				{
					_timer = _bcDuration; //next state is broadcast again. (extension)
					_currentState = STATE_BC;
					_bcCount++; //increase broadcast count
					_isExtending = false;
				}
				else
				{
					if (_isPowerSavingEnabled)
					{
						_intermission_step++;
						if (_intermission_step > INTERMISSION_STEP)//check for step limit to increase intermission
						{
							_bcIntermission *= 2;
							if (_bcIntermission > INTERMISSION_MAX)
								_bcIntermission = INTERMISSION_MAX;

							_intermission_step = 0;
						}
					}


					//CONTROL RUNS
					if (_bcIntermission == 0)
					{
						_timer = _bcDuration;
						_currentState = STATE_BC;
						_bcCount++;
					}
					else
					{
						_timer = _bcIntermission; // next state is intermission
						_currentState = STATE_INT;
					}

				}
				break;
			case STATE_INT:
				_timer = _bcDuration; //next state is broadcast
				_currentState = STATE_BC; 
				_bcCount++;
				break;
		}
	}

	public void increaseBcDuration(int increment)
	{
		_totalBcDuration += increment;
	}

	public boolean meet()
	{
		if (_currentState == STATE_BC)
		{
			_hit++;
			_isExtending = true;
			if (_isPowerSavingEnabled)
			{
				_intermission_step = 0;
				_bcIntermission = INTERMISSION_MIN;
			}
			return true;
		}
		else
			return false;
	}

	public void miss()
	{
		_miss++;
	}

	public String getCSVString()
	{
		return _bcDuration + "," + _bcIntermission + "," + _bcCount + "," + _totalBcDuration + "," + _hit + "," + _miss + "," + (_hit * 100.0f / (_hit + _miss)) +  "\n";
	}
}
