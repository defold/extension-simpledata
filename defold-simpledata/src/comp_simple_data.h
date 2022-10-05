#ifndef DM_GAMESYS_COMP_SIMPLE_DATA_H
#define DM_GAMESYS_COMP_SIMPLE_DATA_H

#include "res_simpledata.h"

namespace dmSimpleData
{
	struct SimpleDataComponent
	{
		SimpleDataResource* m_Resource;
	};

	void GetArrayData(SimpleDataComponent* component, float** data, int* count);
}

#endif // DM_GAMESYS_COMP_SIMPLE_DATA_H
